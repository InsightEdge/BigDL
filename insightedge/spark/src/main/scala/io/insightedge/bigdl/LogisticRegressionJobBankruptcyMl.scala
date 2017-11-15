//   scalastyle:off
package io.insightedge.bigdl

import org.apache.spark.ml.classification.{LogisticRegression, LogisticRegressionSummary}
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.udf


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

    val model = new LogisticRegression().fit(trainingData)

    val results: LogisticRegressionSummary = model.evaluate(testData)
    results.predictions.createOrReplaceTempView("predictions")
    println(results.predictions.show(20))
    spark.stop()
  }

  def getDoubleValue(input: String): Double = input match {
    case "P" => 3.0
    case "A" => 2.0
    case "N" | "NB" => 1.0
    case "B" => 0.0
  }


}
