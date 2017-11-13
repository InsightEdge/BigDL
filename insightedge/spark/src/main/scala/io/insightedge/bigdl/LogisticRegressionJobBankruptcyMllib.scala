//   scalastyle:off
package io.insightedge.bigdl

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
//import org.apache.spark
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.basic._

import scala.language.existentials

import org.apache.spark.mllib.classification.{LogisticRegressionWithLBFGS, LogisticRegressionModel}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.linalg.{Vector, Vectors}


// $example on$

object LogisticRegressionJobBankruptcyMllib {


  def main(args: Array[String]): Unit = {


//    val spark = SparkSession
//      .builder
//      .appName("LogisticRegressionWithElasticNetExample")
//      .getOrCreate()
    val sparkConf = new SparkConf()
      .setAppName("sample-app")
      .setMaster("spark://127.0.0.1:7077")
      .setInsightEdgeConfig(InsightEdgeConfig("insightedge-space"))
    val sc = new SparkContext(sparkConf)

    // $example on$
    // Load training data

    val data: RDD[String] = sc.textFile("/home/dgurin/Downloads/Qualitative_Bankruptcy/Qualitative_Bankruptcy/Qualitative_Bankruptcy.data.txt")
    println(data.count())

    val parsedData: RDD[LabeledPoint] = data.map{ line =>
      val parts = line.split(",")
      LabeledPoint(getDoubleValue(parts(6)), Vectors.dense(parts.slice(0,6).map(x => getDoubleValue(x))))
    }

    println(parsedData.take(10))

    val splits = parsedData.randomSplit(Array(0.6, 0.4), seed = 11L)
    val trainingData: RDD[LabeledPoint] = splits(0)
    val testData = splits(1)

//    val model = new LogisticRegressionWithLBFGS().setNumClasses(2).run(trainingData)
    val model = new LogisticRegressionWithLBFGS().setNumClasses(2).run(trainingData)

    val labelAndPreds: RDD[(Double, Double)] = testData.map { point =>
       val prediction = model.predict(point.features)
      (point.label, prediction)
    }
    labelAndPreds.foreach(r => println(r._1 + " " + r._2))
    val trainErr = labelAndPreds.filter(r => r._1 != r._2).count.toDouble / testData.count

    println(trainErr)
//    spark.stop()
    sc.stop()
  }

  def getDoubleValue(input: String): Double = input match {
    case "P" => 3.0
    case "A" => 2.0
    case "N" | "NB" => 1.0
    case "B" => 0.0
  }


}
