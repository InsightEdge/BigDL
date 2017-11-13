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
import org.apache.spark.mllib.linalg
import org.apache.spark.rdd.RDD
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.basic._

import scala.collection.mutable.{Map => MMap}
import scala.language.existentials


object LinearRegressionJob {

  val FEATURES_DIM = 1
  val data_len = 1000
  val learning_rate = 0.2
  val training_epochs = 1
  val batch_size = 4
  val n_input = FEATURES_DIM
  val n_output = 1

  val trainingSplit: Double = 0.8
  def maxSequenceLength: Int = 1000
  def embeddingDim: Int = 100


  val r = scala.util.Random

  // TODO not sure about correctness of data generation
  def generateRandomSample(): Sample[(Float)] = {
//    x -> input
//    y -> output
    val features: Array[Float] = Array(r.nextFloat()) // x
    val label: Float = features.sum * 2f + 0.4f // y
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
      .setAppName("Linear Regression")
      .set("spark.task.maxFailures", "1").setInsightEdgeConfig(ieConfig)
    val sc = SparkContext.getOrCreate(conf)
    Engine.init

    val sample = (0 to data_len).map(_ => generateRandomSample()).toList
    val sampleRDD: RDD[Sample[Float]] = sc.parallelize(sample)
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
      if (p.isTable) {
        println("Should not be here")
      } else {
        val tensor = p.toTensor[Float]
//        val sizes: Array[Int] = tensnor.size()
//        println("Tensor size: " + sizes.deep.mkString(", "))
        println("Tensor: " + tensor)
//        val vector: linalg.Vector = tensor.toMLlibVector()
//        println("List: " + vector.toArray.toList)

        println("Value at 0: " + tensor(0))
        println("Value at 0: " + tensor.valueAt(0))
        println("Value at 1: " + tensor.valueAt(1))
      }
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
