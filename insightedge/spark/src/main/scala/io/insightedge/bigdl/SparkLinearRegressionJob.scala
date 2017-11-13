//   scalastyle:off
package io.insightedge.bigdl

import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.regression.{LinearRegression, LinearRegressionModel}
import org.apache.spark.mllib
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SparkSession
import org.apache.spark.mllib.linalg.Vectors


//object SparkLinearRegressionJob {
//
//  def main(args: Array[String]): Unit = {
//    val ieConfig = InsightEdgeConfig("insightedge-space", Some("insightedge"), Some("127.0.0.1:4174"))
//    val conf = Engine.createSparkConf()
//      .setAppName("Linear Regression")
//      .set("spark.task.maxFailures", "1").setInsightEdgeConfig(ieConfig)
//    val sc = SparkContext.getOrCreate(conf)
//
//    val training = spark.read.format("libsvm")
//      .load("/code/bigdl-fork/BigDL/data/regression/dataStock.txt")
//    training.printSchema()
//
//    val lr = new LinearRegression()
//      .setMaxIter(40)
//      .setRegParam(0.3)
//      .setElasticNetParam(0.8)
//
//    val lrModel: LinearRegressionModel = lr.fit(training)
//    val trainingSummary = lrModel.summary
//    println(s"numIterations: ${trainingSummary.totalIterations}")
//    println(s"objectiveHistory: [${trainingSummary.objectiveHistory.mkString(",")}]")
//
//    import spark.sqlContext.implicits._
//    val xs = 1 until 7
//    val values = spark.sparkContext.parallelize(xs).toDF("vals")
//
//    val assembler =  new VectorAssembler()
//      .setInputCols(Array("vals"))
//      .setOutputCol("features")
//
//    val test: DataFrame = assembler.transform(values)
//    test.printSchema()
//    test.show()
//
//    println("=----- transform")
//    val summary: DataFrame = lrModel.transform(test)
//    summary.show()
//
//    val predictions = summary.select("prediction").collect().map(_.apply(0)).map(value => math.sqrt(value.toString.toDouble)).toList
//    println(predictions)
//
//
//    sc.stop()
//
//  }
//
//}
