//   scalastyle:off
package io.insightedge.bigdl

import org.apache.spark.ml.classification.{LogisticRegression, LogisticRegressionSummary}
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions.udf

import scala.language.existentials


// $example on$

object LogisticRegressionJobBankruptcyMl {


  def main(args: Array[String]): Unit = {


    val spark = SparkSession
      .builder
      .appName("LogisticRegressionBankruptcyMl")
      .getOrCreate()

    val data: RDD[String] = spark.sparkContext.textFile("/home/dgurin/Downloads/Qualitative_Bankruptcy/Qualitative_Bankruptcy/Qualitative_Bankruptcy.data.txt")
    println(data.count())

    val schemaString = "label features_raw"

    val fields = schemaString.split(" ")
      .map(fieldName => StructField(fieldName, DoubleType))
    val schema = StructType(Array(StructField("label", DoubleType), StructField("features_raw", StringType)))

    val toUdfVector: UserDefinedFunction = udf[Vector, String] { x =>
      val parts = x.split(",")
      val features: Array[String] = parts.slice(0, 6)
      Vectors.dense(features.map(s => getDoubleValue(s)))
    }

    val parsedData: RDD[Row] = data.map { line =>
      val parts = line.split(",")
      Row(getDoubleValue(parts(6)), line)
    }

    val df = spark.createDataFrame(parsedData, schema)
    df.printSchema()

    val df2 = df.withColumn("features_raw", toUdfVector(df("features_raw")))
    val assembler = new VectorAssembler().
      setInputCols(Array("features_raw")).
      setOutputCol("features")

    val out = assembler.transform(df2)
    out.printSchema()

    val splits = out.randomSplit(Array(0.6, 0.4), seed = 11L)
    val trainingData = splits(0)
    val testData = splits(1)

    trainingData.printSchema()

    val model = new LogisticRegression().fit(trainingData)

    val results: LogisticRegressionSummary = model.evaluate(testData)
    results.predictions.createOrReplaceTempView("predictions")
    println(results.predictions.show(20))
    println("labels " + results.labelCol)
    println("features " + results.featuresCol)
    println("Propbs " + results.probabilityCol)
    spark.stop()
  }

  def getDoubleValue(input: String): Double = input match {
    case "P" => 3.0
    case "A" => 2.0
    case "N" | "NB" => 1.0
    case "B" => 0.0
  }


}
