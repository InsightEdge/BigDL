//   scalastyle:off
package io.insightedge.bigdl

import com.intel.analytics.bigdl._
import com.intel.analytics.bigdl.dataset.Sample
import com.intel.analytics.bigdl.nn._
import com.intel.analytics.bigdl.nn.abstractnn.Activity
import com.intel.analytics.bigdl.optim.{Optimizer, Trigger, _}
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.utils.Engine
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.basic._

import scala.collection.mutable.{Map => MMap}
import scala.language.existentials


object LinearRegression {


  val FEATURES_DIM = 2
  val data_len = 100
  val learning_rate = 0.2
  val training_epochs = 5
  val batch_size = 4
  val n_input = FEATURES_DIM
  val n_output = 1

  val trainingSplit: Double = 0.8
  def maxSequenceLength: Int = 1000
  def embeddingDim: Int = 100


  val r = scala.util.Random

  // TODO not sure about correctness of data generation
  def generateRandomSample(): Sample[(Float)] = {
    val features: Array[Float] = Array(r.nextFloat(), r.nextFloat())
    val label: Float = features.sum * 2f + 0.4f
    val tensor: Tensor[Float] = Tensor(features, Array(FEATURES_DIM))
    val sample: Sample[Float] = Sample(tensor, label)
    sample
  }

  def buildLinearRegressionModel(input: Int, output: Int  ): Sequential[Float] = {
    val model = Sequential[Float]()
    model.add(Linear(input, output))
    model
  }

  def main(args: Array[String]): Unit = {
    val ieConfig = InsightEdgeConfig("insightedge-space", Some("insightedge"), Some("127.0.0.1:4174"))
    val conf = Engine.createSparkConf()
      .setAppName("Text classification")
      .set("spark.task.maxFailures", "1").setInsightEdgeConfig(ieConfig)
    val sc = SparkContext.getOrCreate(conf)
    Engine.init

    val sampleRDD: RDD[Sample[Float]] = sc.parallelize(0 to 1000).map(_ => generateRandomSample())
    val Array(trainingRDD, valRDD) = sampleRDD.randomSplit(
      Array(trainingSplit, 1 - trainingSplit))

    val model = buildLinearRegressionModel(n_input, n_output)

    val optimizer = Optimizer(
      model = model,
      sampleRDD = trainingRDD,
      criterion = new MSECriterion[Float](),
      batchSize = batch_size)
      .setOptimMethod(new SGD[Float](learningRate = learning_rate))
      .setEndWhen(Trigger.maxEpoch(training_epochs))

    val trainedModel: Module[Float] = optimizer.optimize()

    val prediction: RDD[Activity] = trainedModel.predict(valRDD)

    prediction.take(10).foreach { p =>
      println("Prediction: " + p)
    }

    valRDD.take(10).toList.foreach {
      i =>
      println("data: " + i.getData().deep.mkString(", "))
      println("features: " + i.getFeatureSize().deep.mkString(", "))
      println("labels: " + i.getLabelSize().deep.mkString(", "))
    }

    sc.stop()

  }

}
