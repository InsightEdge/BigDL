//   scalastyle:off
package io.insightedge.bigdl

import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.{SparkConf, SparkContext}
//import org.apache.spark
import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS
import org.apache.spark.mllib.regression.LabeledPoint
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.basic._

import scala.language.existentials


// $example on$

object LogisticRegressionJobBankruptcyMl {


  def main(args: Array[String]): Unit = {


    val spark = SparkSession
      .builder
      .appName("LogisticRegressionWithElasticNetExample")
      .getOrCreate()
//    val sparkConf = new SparkConf()
//      .setAppName("sample-app")
//      .setMaster("spark://127.0.0.1:7077")
//      .setInsightEdgeConfig(InsightEdgeConfig("insightedge-space"))
//    val sc = new SparkContext(sparkConf)

    // $example on$
    // Load training data

    val data: RDD[String] = spark.sparkContext.textFile("/home/dgurin/Downloads/Qualitative_Bankruptcy/Qualitative_Bankruptcy/Qualitative_Bankruptcy.data.txt")
    println(data.count())

    val schemaString = "label features"

    // Generate the schema based on the string of schema
    val fields = schemaString.split(" ")
      .map(fieldName => StructField(fieldName, DoubleType))
    val schema = StructType(fields)

    val parsedData  = data.map{ line =>
      val parts = line.split(",")
      val vector = Vectors.dense(parts.slice(0, 6).map(x => getDoubleValue(x)))
      Row(getDoubleValue(parts(6)), vector)
    }

    import org.apache.spark.sql.functions._


    val assembler: VectorAssembler = new VectorAssembler()
      .setInputCols(Array("label", "feature"))
      .setOutputCol("features")


//    val toVec4 = udf[Vector, Int, Int, String, String] { (a,b,c,d) =>
//      val e3 = c match {
//        case "hs-grad" => 0
//        case "bachelors" => 1
//        case "masters" => 2
//      }
//      val e4 = d match {case "male" => 0 case "female" => 1}
//      Vectors.dense(a, b, e3, e4)
//    }

    println(parsedData.take(10))

    val splits = parsedData.randomSplit(Array(0.6, 0.4), seed = 11L)
    val trainingData = spark.createDataFrame(splits(0),schema)
    val testData = spark.createDataFrame(splits(1), schema)

    trainingData.printSchema()

//    val model = new LogisticRegressionWithLBFGS().setNumClasses(2).run(trainingData)
    val model = new LogisticRegression().fit(trainingData)

//    val labelAndPreds: RDD[(Double, Double)] = testData.map { point =>
//       val prediction = model.evaluate(point.features)
//      (point.label, prediction)
//    }

    val results = model.evaluate(testData)
    println(results)
//    labelAndPreds.foreach(r => println(r._1 + " " + r._2))
//    val trainErr = labelAndPreds.filter(r => r._1 != r._2).count.toDouble / testData.count
//
//    println(trainErr)
//    spark.stop()
    spark.stop()
  }

  def getDoubleValue(input: String): Double = input match {
    case "P" => 3.0
    case "A" => 2.0
    case "N" | "NB" => 1.0
    case "B" => 0.0
  }


}
