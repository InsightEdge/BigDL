//   scalastyle:off
package io.insightedge.bigdl

import org.apache.spark.ml.classification.LogisticRegressionModel
import org.apache.spark.sql.{DataFrame, SparkSession}
//import org.apache.spark
import org.apache.spark.sql.functions.max

import scala.collection.mutable.{Map => MMap}
import scala.language.existentials

// $example on$
import org.apache.spark.ml.classification.{BinaryLogisticRegressionSummary, LogisticRegression}

object LogisticRegressionJob {


  def main(args: Array[String]): Unit = {


    val spark = SparkSession
      .builder
      .appName("LogisticRegressionWithElasticNetExample")
      .getOrCreate()

    import spark.implicits._
//    val sparkConf = new SparkConf()
//      .setAppName("sample-app")
//      .setMaster("spark://127.0.0.1:7077")
//      .setInsightEdgeConfig(InsightEdgeConfig("insightedge-space"))
//    val sc = new SparkContext(sparkConf)

    // $example on$
    // Load training data
    val training: DataFrame = spark.read.format("libsvm").load("/home/dgurin/Downloads/gigaspaces-insightedge-2.1.0-ga-100-premium/data/mllib/sample_libsvm_data.txt")

    training.printSchema()
    val lr = new LogisticRegression()
      .setMaxIter(10)
      .setRegParam(0.3)
      .setElasticNetParam(0.8)

    // Fit the model
    val lrModel: LogisticRegressionModel = lr.fit(training)

    // We can also use the multinomial family for binary classification
    val mlr = new LogisticRegression()
      .setMaxIter(10)
      .setRegParam(0.3)
      .setElasticNetParam(0.8)
      .setFamily("multinomial")

    val mlrModel: LogisticRegressionModel = mlr.fit(training)

    // Print the coefficients and intercept for logistic regression
    println(s"Coefficients: ${lrModel.coefficients} Intercept: ${lrModel.intercept}")
    // Print the coefficients and intercepts for logistic regression with multinomial family
    println(s"Multinomial coefficients: ${mlrModel.coefficientMatrix}")
    println(s"Multinomial intercepts: ${mlrModel.interceptVector}")
    // $example off$


    val trainingSummary = lrModel.summary

    // Obtain the objective per iteration.
    val objectiveHistory = trainingSummary.objectiveHistory
    println("objectiveHistory:")
    objectiveHistory.foreach(loss => println(loss))

    // Obtain the metrics useful to judge performance on test data.
    // We cast the summary to a BinaryLogisticRegressionSummary since the problem is a
    // binary classification problem.
    val binarySummary = trainingSummary.asInstanceOf[BinaryLogisticRegressionSummary]

    // Obtain the receiver-operating characteristic as a dataframe and areaUnderROC.
    val roc = binarySummary.roc
    roc.show()
    println(s"areaUnderROC: ${binarySummary.areaUnderROC}")

    // Set the model threshold to maximize F-Measure
    val fMeasure = binarySummary.fMeasureByThreshold
    val maxFMeasure = fMeasure.select(max("F-Measure")).head().getDouble(0)
    val bestThreshold = fMeasure.where($"F-Measure" === maxFMeasure)
      .select("threshold").head().getDouble(0)
    lrModel.setThreshold(bestThreshold)



    spark.stop()
  }

//  def main(args: Array[String]): Unit = {
//    val ieConfig = InsightEdgeConfig("insightedge-space", Some("insightedge"), Some("127.0.0.1:4174"))
//    val conf = Engine.createSparkConf()
//      .setAppName("Linear Regression")
//      .set("spark.task.maxFailures", "1").setInsightEdgeConfig(ieConfig)
//    val sc = SparkContext.getOrCreate(conf)
//
//
//    import org.apache.spark.ml.classification.LogisticRegression
//
//    val spark = SparkSession.builder.appName("Simple Application").getOrCreate()
//    // Load training data
//    val training = spark.read.format("libsvm").load("/home/dgurin/Downloads/gigaspaces-insightedge-2.1.0-ga-100-premium/data/mllib/sample_libsvm_data.txt")
//
//    val lr = new LogisticRegression()
//      .setMaxIter(10)
//      .setRegParam(0.3)
//      .setElasticNetParam(0.8)
//
//    // Fit the model
//    val lrModel = lr.fit(training)
//
//    // Print the coefficients and intercept for logistic regression
//    println(s"Coefficients: ${lrModel.coefficients} Intercept: ${lrModel.intercept}")
//
//    // We can also use the multinomial family for binary classification
//    val mlr = new LogisticRegression()
//      .setMaxIter(10)
//      .setRegParam(0.3)
//      .setElasticNetParam(0.8)
//      .setFamily("multinomial")
//
//    val mlrModel = mlr.fit(training)
//
//    // Print the coefficients and intercepts for logistic regression with multinomial family
//
//    mlrModel.coefficientMatrix.toArray.foreach(println)
//    println(s"Multinomial coefficients: ${mlrModel.coefficientMatrix}")
//    println(s"Multinomial intercepts: ${mlrModel.interceptVector}")
//
//
//    sc.stop()
//
//  }

}
