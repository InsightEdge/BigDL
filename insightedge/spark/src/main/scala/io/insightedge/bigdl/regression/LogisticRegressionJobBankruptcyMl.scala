//   scalastyle:off
package io.insightedge.bigdl.regression

import _root_.kafka.serializer.StringDecoder
import org.apache.spark.SparkConf
import org.apache.spark.ml.classification.{LogisticRegression, LogisticRegressionSummary}
import org.apache.spark.ml.linalg.{DenseVector, Vector, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.basic._


object LogisticRegressionJobBankruptcyMl {


  def main(args: Array[String]): Unit = {


    val gsConfig = InsightEdgeConfig("insightedge-space", Some("insightedge"), Some("127.0.0.1:4174"))
    val conf = new SparkConf()
      .setAppName("LogisticRegressionBankruptcyMl")
      .set("spark.task.maxFailures", "1").setInsightEdgeConfig(gsConfig)

    val spark = SparkSession
      .builder
      .config(conf)
      .getOrCreate()

    val rawData: RDD[String] = spark.sparkContext.textFile("/home/dgurin/Downloads/Qualitative_Bankruptcy/Qualitative_Bankruptcy/Qualitative_Bankruptcy.data.txt")
    println(rawData.count())

    val schema = StructType(Array(StructField("label", DoubleType), StructField("features_raw", StringType)))


    val toUdfVector: UserDefinedFunction = udf[Vector, String] { x =>
      val parts = x.split(",")
      val features: Array[String] = parts.slice(0, 6)
      Vectors.dense(features.map(s => getDoubleValue(s)))
    }

    val rawRddData: RDD[Row] = rawData.map { line =>
      val parts = line.split(",")
      Row(getDoubleValue(parts(6)), line)
    }

    val df = spark.createDataFrame(rawRddData, schema)

    val inputData = df.withColumn("features", toUdfVector(df("features_raw")))
    inputData.printSchema()

    val splits = inputData.randomSplit(Array(0.6, 0.4), seed = 11L)
    val trainingData = splits(0)
    val testData = splits(1)

    val model = new LogisticRegression().setMaxIter(10)
      .setRegParam(0.3)
      .setElasticNetParam(0.8)
      .fit(inputData)

    import spark.implicits._
    val x = model.summary.predictions.map { row =>
      val inputData = row.getString(1)
      val features = row.getAs[DenseVector](2).toArray.mkString(",")
      val prediction = row.getDouble(5)
      val probabilities = row.getAs[DenseVector](4).toArray.mkString(",")

      TrainingResult(null, inputData, features, prediction, probabilities)
    }
//    spark.sparkContext.grid.clear(TrainingResult)
    spark.sparkContext.saveMultipleToGrid(x.rdd.collect())

//    val results: LogisticRegressionSummary = model.evaluate(testData)
//    results.predictions.createOrReplaceTempView("predictions")
//    println(results.predictions.show(20))

    val ssc = new StreamingContext(spark.sparkContext, Seconds(1))

    val (brokers, topics) = "localhost:9092" -> "bankruptcy"
    val topicsSet = topics.split(",").toSet
    val kafkaParams: Map[String, String] = Map[String, String]("metadata.broker.list" -> brokers)

    val messages: InputDStream[(String, String)] = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topicsSet)
    println("Waiting for input...")

    val toUdfVectorWithError: UserDefinedFunction = udf[Vector, String] { x =>
      val parts = x.split(",")
      val features: Array[String] = parts.slice(0, 6)
      Vectors.dense(features.map(s => getDoubleValueWithError(s)))
    }


    val schemaKafka = StructType(Array(StructField("features_raw", StringType)))
    messages.foreachRDD{(rdd: RDD[(String, String)]) =>
      if (!rdd.isEmpty) {
        println("-------------------------")
        val textRdd: RDD[String] = rdd.map(_._2)

        val dfRaw = spark.createDataFrame(textRdd.map(l => Row(l)), schemaKafka)
        val inputDataKafka = dfRaw.withColumn("features", toUdfVectorWithError(dfRaw("features_raw")))

        //        inputDataKafka.show

        //        val streamResult = model.evaluate(inputDataKafka)
        val streamResult: DataFrame = model.transform(inputDataKafka)

        //        streamResult.show(false)

        val predictions = streamResult.collect.map { row =>
          val input = row.getString(0)
          val prediction = row.getDouble(4)
          val probabilities = row.getAs[DenseVector](3).toArray.map(d => BigDecimal(d * 100).setScale(0, BigDecimal.RoundingMode.HALF_UP).toString.split("\\.")(0))
          val inputP = input.split(",").map(_.split("\\.")(0))
          println(s"Input data \n" +
            s"    Industrial Risk: ${inputP(0)}%\n" +
            s"    Management Risk: ${inputP(1)}%\n" +
            s"    Financial Flexibility: ${inputP(2)}%\n" +
            s"    Credibility: ${inputP(3)}%\n" +
            s"    Competitiveness: ${inputP(4)}%\n" +
            s"    Operating Risk: ${inputP(5)}%")
          println("Prediction: " + prediction + " (" + getPredictionValue(prediction) + ")")
          println("Probabilities: " +
            "Bankruptcy - " + probabilities(0) + "% " +
            ", Non-bankruptcy - " + probabilities(1) + "%"
          )

          println("")
          Prediction(null, input, prediction, probabilities.mkString(","))
        }
        spark.sparkContext.saveMultipleToGrid(predictions)
      }
    }

    ssc.start()
    ssc.awaitTermination()
    //    spark.stop()
  }

  def getDoubleValueWithError(input: String): Double = {
    input.toDouble
  }

  def getDoubleValue(input: String): Double = input match {
    case "P" => 100.0
    case "A" => 50.0
    case "N" => 0.0
    case "NB" => 1.0
    case "B" => 0.0
  }


  def getPredictionValue(input: Double): String = input match {
    case 1.0d => "Non-bankruptcy"
    case 0.0d => "Bankruptcy"
  }

}
