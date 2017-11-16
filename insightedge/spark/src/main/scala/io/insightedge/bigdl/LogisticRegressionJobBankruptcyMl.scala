//   scalastyle:off
package io.insightedge.bigdl

import _root_.kafka.serializer.StringDecoder
import org.apache.spark.ml.classification.{LogisticRegression, LogisticRegressionSummary}
import org.apache.spark.ml.linalg.{DenseVector, Vector, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.udf
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}


object LogisticRegressionJobBankruptcyMl {


  def main(args: Array[String]): Unit = {


    val spark = SparkSession
      .builder
      .appName("LogisticRegressionBankruptcyMl")
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

    val results: LogisticRegressionSummary = model.evaluate(testData)
    results.predictions.createOrReplaceTempView("predictions")
    println(results.predictions.show(20))

    val ssc = new StreamingContext(spark.sparkContext, Seconds(1))

    val (brokers, topics) = "localhost:9092" -> "bankruptcy"
    val topicsSet = topics.split(",").toSet
    val kafkaParams: Map[String, String] = Map[String, String]("metadata.broker.list" -> brokers)

    val messages: InputDStream[(String, String)] = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topicsSet)
    println("reading texts")

    val schemaKafka = StructType(Array(StructField("features_raw", StringType)))
    messages.foreachRDD{(rdd: RDD[(String, String)]) =>
      if (!rdd.isEmpty) {
        println("-------------------------")
        val textRdd: RDD[String] = rdd.map(_._2)

        val dfRaw = spark.createDataFrame(textRdd.map(l => Row(0.0d, l)), schema) //TODO why do we need to specify label column?
        val inputDataKafka = dfRaw.withColumn("features", toUdfVector(dfRaw("features_raw")))

//        inputDataKafka.show

//        val streamResult = model.evaluate(inputDataKafka)
        val streamResult: DataFrame = model.transform(inputDataKafka)

        streamResult.show(false)

        streamResult.collect.foreach { row =>
          val input = row.getString(1)
          val prediction = row.getDouble(5)
          val probabilities = row.getAs[DenseVector](4).toArray

          println("Input data: " + input)
          println("Prediction: " + prediction + " (" + getPredictionValue(prediction) + ")")
          println("Probabilities: " +
            "Bankruptcy - " + probabilities(0) +
            ", Non-bankruptcy - " + probabilities(1)
          )

          println("")
        }
      }
    }

    ssc.start()
    ssc.awaitTermination()
    //    spark.stop()
  }

  def getDoubleValue(input: String): Double = input match {
    case "P" => 3.0
    case "A" => 2.0
    case "N" | "NB" => 1.0
    case "B" => 0.0
  }

  def getPredictionValue(input: Double): String = input match {
    case 1.0d => "Non-bankruptcy"
    case 0.0d => "Bankruptcy"
  }



}
