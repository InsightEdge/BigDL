//   scalastyle:off

package io.insightedge.bigdl

import java.io.File
import java.util

import _root_.kafka.serializer.StringDecoder
import com.gigaspaces.document.SpaceDocument
import com.intel.analytics.bigdl._
import com.intel.analytics.bigdl.dataset._
import com.intel.analytics.bigdl.example.utils.SimpleTokenizer._
import com.intel.analytics.bigdl.example.utils.{SimpleTokenizer, WordMeta}
import com.intel.analytics.bigdl.nn.{ClassNLLCriterion, _}
import com.intel.analytics.bigdl.optim._
import com.intel.analytics.bigdl.tensor.Tensor
import com.intel.analytics.bigdl.utils.Engine
import com.j_spaces.core.client.SQLQuery
import io.insightedge.bigdl.model._
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.insightedge.spark.context.InsightEdgeConfig
import org.insightedge.spark.implicits.basic._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.collection.mutable.{ArrayBuffer, Map => MMap}
import scala.io.Source
import scala.language.existentials
import com.intel.analytics.bigdl.visualization.ValidationSummary
import com.intel.analytics.bigdl.visualization.TrainSummary
import org.insightedge.spark.utils.GridProxyFactory


/**
  * @author Danylo_Hurin.
  */
class InsightedgeTextClassifier(param: IeAbstractTextClassificationParams) extends Serializable {

  val log: Logger = LoggerFactory.getLogger(this.getClass)
  val gloveDir = s"${param.baseDir}/glove.6B/"
  val textDataDir = s"${param.baseDir}/20_newsgroup/"
  var classNum = -1

  /**
    * Start to train the text classification model
    */
  def train(): Unit = {
    val gsConfig = InsightEdgeConfig("insightedge-space", Some("insightedge"), Some("127.0.0.1:4174"))
    val conf = Engine.createSparkConf()
      .setAppName("Text classification")
      .set("spark.task.maxFailures", "1").setInsightEdgeConfig(gsConfig)
    val sc = SparkContext.getOrCreate(conf)

    initSpaceObjects(sc)
    log.info("Space objects were initialized")

    Engine.init
    val sequenceLen = param.maxSequenceLength
    val embeddingDim = param.embeddingDim //depends on which file is chosen for training. 50d -> 50, 100d -> 100
    val trainingSplit = param.trainingSplit
    val modelFile = param.modelFile
    val epochNum = param.epochNum
    val gloveEmbeddingsFile = s"$gloveDir/glove.6B.${embeddingDim}d.txt"

    log.info(s"Embeddings dimensions: $embeddingDim")
    log.info(s"Epochs number: $epochNum")

    val (textToLabel: Seq[(String, Int)], categoriesMapping: Map[String, Int]) = loadRawData()
    overwriteCategories(sc, categoriesMapping)
    overwriteTrainingText(sc, textToLabel)

    val textToLabelFloat: Seq[(String, Float)] = textToLabel.map { case (text, label) => (text, label.toFloat) }
    // For large dataset, you might want to get such RDD[(String, Float)] from HDFS
    // String - text, Float - index of category
    val textToLabelRdd: RDD[(String, Float)] = sc.parallelize(textToLabelFloat, param.partitionNum)
    val (word2Meta: Map[String, WordMeta], word2Vec) = analyzeTexts(textToLabelRdd, gloveEmbeddingsFile)

    overwriteWordMetainfo(sc, word2Meta.map { case (word, meta) =>
      WordMetainfo(null, word, meta.count, meta.index)
    }.toSeq)

    val word2MetaBC: Broadcast[Map[String, WordMeta]] = sc.broadcast(word2Meta)
    val word2VecBC: Broadcast[Map[Float, Array[Float]]] = sc.broadcast(word2Vec)

    val tokensToLabel: RDD[(Array[Float], Float)] = textToLabelRdd.map { case (text, label) => (toTokens(text, word2MetaBC.value), label) }
    val shapedTokensToLabel: RDD[(Array[Float], Float)] = tokensToLabel.map { case (tokens, label) => (shaping(tokens, sequenceLen), label) }
    val vectorizedRdd: RDD[(Array[Array[Float]], Float)] = shapedTokensToLabel
      .map { case (tokens, label) => (vectorization(tokens, embeddingDim, word2VecBC.value), label) }

    val sampleRDD: RDD[Sample[Float]] = vectorizedRdd.map { case (input: Array[Array[Float]], label: Float) =>
      val flatten: Array[Float] = input.flatten
      val shape: Array[Int] = Array(sequenceLen, embeddingDim)
      val tensor: Tensor[Float] = Tensor(flatten, shape).transpose(1, 2).contiguous()
      Sample(featureTensor = tensor, label = label)
    }

    val Array(trainingRDD, validationRDD) = sampleRDD.randomSplit(
      Array(trainingSplit, 1 - trainingSplit))

    val optimizer = Optimizer(
      model = buildModel(classNum),
      sampleRDD = trainingRDD,
      criterion = new ClassNLLCriterion[Float](),
      batchSize = param.batchSize
    )

    val logdir = "/tmp/text_classifier"
    val trainSummary = TrainSummary(logdir, "Text classification")
    val validationSummary = ValidationSummary(logdir, "Text classification")
    optimizer.setTrainSummary(trainSummary)
    optimizer.setValidationSummary(validationSummary)

    val start = System.currentTimeMillis()
    val trainedModel: Module[Float] = optimizer
      .setOptimMethod(new Adagrad(learningRate = 0.01, learningRateDecay = 0.0002))
      .setValidation(Trigger.everyEpoch, validationRDD, Array(new Top1Accuracy[Float]), param.batchSize)
      .setEndWhen(Trigger.maxEpoch(epochNum))
      .optimize()
    val stop = System.currentTimeMillis()

    // TODO save to the grid as binary data: @SpaceStorageType(storageType = StorageType.BINARY)
    trainedModel.save(modelFile, overWrite = true)
    log.info(s"Model was saved to $modelFile")

    val array: Array[ValidationMethod[Float]] = Array(new Top1Accuracy[Float])
    val accuracy = trainedModel.evaluate(validationRDD, array)(0)._1.result()._1
    overwriteModelStats(sc, stop - start, accuracy)

    sc.stop()
  }

  def predictFromStream(): Unit = {
    val gsConfig = InsightEdgeConfig("insightedge-space", Some("insightedge"), Some("127.0.0.1:4174"))
    val conf = Engine.createSparkConf()
      .setAppName("Text classification")
      .set("spark.task.maxFailures", "1").setInsightEdgeConfig(gsConfig)
    val sc = SparkContext.getOrCreate(conf)

    val broadcasterIeConf = sc.broadcast(gsConfig)

    val categories = sc.broadcast(sc.gridRdd[Category]().collect())
    log.info(s"${categories.value.length} were loaded")

    Engine.init
    val sequenceLen = param.maxSequenceLength
    val embeddingDim = param.embeddingDim //depends on which file is chosen for training. 50d -> 50, 100d -> 100
    val modelFile = param.modelFile

    log.info("Engine init")
    val trainedModel = sc.broadcast(Module.load[Float](modelFile))
    log.info(s"Model was loaded from $modelFile and broadcasted")

    val (brokers, topics) = "localhost:9092" -> "texts"
    val ssc = new StreamingContext(sc, Seconds(5))
    val topicsSet = topics.split(",").toSet
    val kafkaParams: Map[String, String] = Map[String, String]("metadata.broker.list" -> brokers)

    val messages: InputDStream[(String, String)] = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topicsSet)

    log.info("reading texts")

    val gloveEmbeddingsFile = s"$gloveDir/glove.6B.${embeddingDim}d.txt"
    val file = Source.fromFile(gloveEmbeddingsFile, "ISO-8859-1")
    // TODO format embeddings
    val embeddings = sc.broadcast(file.getLines().toList)
    file.close

    val metainfo = sc.gridRdd[WordMetainfo]()
    val word2MetaBC = sc.broadcast(metainfo.map(i => i.word -> WordMeta(i.count, i.wordIndex)).collect().toMap)
    val word2Vec = buildWord2Vec(word2MetaBC.value, embeddings.value)
    val word2VecBC: Broadcast[Map[Float, Array[Float]]] = sc.broadcast(word2Vec)

    val deleted = deleteInProcessCalls(sc)
    log.info(s"Deleted $deleted old in-process calls")

    log.info("Ready to classify...")

    messages.foreachRDD((rdd: RDD[(String, String)]) =>
      if (!rdd.isEmpty) {
        println("-------------------------")
        val textRdd: RDD[String] = rdd.map(_._2)
        val idRdd: RDD[String] = rdd.map(_._1)

        val start = System.currentTimeMillis()
        val tokensToLabel: RDD[Array[Float]] = textRdd.map { text => toTokens(text, word2MetaBC.value) }

        val shapedTokensToLabel: RDD[Array[Float]] = tokensToLabel.map { tokens => shaping(tokens, sequenceLen) }
        val vectorizedRdd: RDD[Array[Array[Float]]] = shapedTokensToLabel
          .map { tokens => vectorization(tokens, embeddingDim, word2VecBC.value) }
        val sampleRDD: RDD[Sample[Float]] = vectorizedRdd.map { input: Array[Array[Float]] =>
          val flatten: Array[Float] = input.flatten
          val shape: Array[Int] = Array(sequenceLen, embeddingDim)
          val tensor: Tensor[Float] = Tensor(flatten, shape).transpose(1, 2).contiguous()
          Sample(featureTensor = tensor)
        }

        val results: RDD[Int] = trainedModel.value.predictClass(sampleRDD)

        val stop = System.currentTimeMillis()
        val totalTime = stop - start
        log.info(s"Total classification time: $totalTime")
        val count = idRdd.count()
        val predictions = textRdd.zip(results).map { case (text, prediction) =>
          val category = categories.value.filter(_.label == prediction)(0)
          Prediction(null, text, category.name, prediction, totalTime / count, 0)
        }
        predictions.saveToGrid()

        if (!idRdd.isEmpty()) {
          val space = GridProxyFactory.getOrCreateClustered(broadcasterIeConf.value)
          val query = new SQLQuery[InProcessCall](classOf[InProcessCall], "id IN (?)")
          val ids = idRdd.collect().toList
          log.info(s"Deleting next inprocess calls: " + ids.mkString(","))
          query.setParameter(1, ids.asJava)
          space.takeMultiple(query)
        }

        log.info(s"Saved predictions to the grid")
        println("-------------------------")
      }
    )

    ssc.start()
    ssc.awaitTermination()
  }

  def initSpaceObjects(sc: SparkContext): Unit = {
    val dummyId = "dummy_id"
    val query = new SQLQuery[CallSession](classOf[CallSession], "id = ?")
    query.setParameter(1, dummyId)
    sc.grid.takeIfExists(query)
    val cs = CallSession(dummyId, "session for initialization", null, null, -1, -1)
    sc.grid.write(cs)
    sc.grid.clear(query)

    val inProcessCall = InProcessCall(null, "call for initialization")
    val newId = sc.grid.write(inProcessCall).getUID
    sc.grid.clear(InProcessCall(newId, "call for initialization"))
  }

  def deleteInProcessCalls(sc: SparkContext): Int = {
    val ids = sc.gridRdd[InProcessCall]().collect().toList.map(_.id)
    val query = new SQLQuery[InProcessCall](classOf[InProcessCall], "id IN (?)")
    query.setParameter(1, ids.asJava)
    sc.grid.takeMultiple(query).length
  }

  def overwriteCategories(sc: SparkContext, categoriesMapping: Map[String, Int]): Unit = {
    val query = new SQLQuery[Category](classOf[Category], "label > 0")
    sc.grid.clear(query)

    val categories = categoriesMapping.map { case (name, label) => Category(null, name, label) }
    sc.parallelize(categories.toList).saveToGrid()
    log.info(s"Saved ${categories.size} categories to the grid")
  }

  def overwriteWordMetainfo(sc: SparkContext, wordMetainfos: Seq[WordMetainfo]): Unit = {
    val query = new SQLQuery[WordMetainfo](classOf[WordMetainfo], "wordIndex >= 0")
    sc.grid.clear(query)

    sc.parallelize(wordMetainfos).saveToGrid()
    log.info(s"Saved ${wordMetainfos.size} word metainfo to the grid")
  }

  def overwriteTrainingText(sc: SparkContext, textToLabel: Seq[(String, Int)]): Unit = {
    val query = new SQLQuery[TrainingText](classOf[TrainingText], "label > 0")
    sc.grid.clear(query)

    val texts = textToLabel.map{ case (text, label) => TrainingText(null, text, label) }
    sc.parallelize(texts).saveToGrid()
    log.info(s"Saved ${texts.length} texts to the grid")
  }

  def overwriteModelStats(sc: SparkContext, time: Long, accuracy: Float): Unit = {
    val query = new SQLQuery[TrainedModelStats](classOf[TrainedModelStats], "timeInMilliseconds >= 0")
    sc.grid.clear(query)

    val stats = TrainedModelStats(null, time, accuracy)
    sc.grid.write(stats)
    log.info(s"Saved trained model statistics: $stats")
  }

  /**
    * Load the pre-trained word2Vec
    *
    * @return A map from word to vector
    */
  def buildWord2Vec(word2Meta: Map[String, WordMeta], gloveEmbeddingsFile: String): Map[Float, Array[Float]] = {
    log.info("Indexing word vectors.")
    val preWord2Vec = MMap[Float, Array[Float]]()
    for (line <- Source.fromFile(gloveEmbeddingsFile, "ISO-8859-1").getLines) {
      val wordAndVectorValues: Array[String] = line.split(" ")
      val word: String = wordAndVectorValues(0)
      if (word2Meta.contains(word)) {
        val vectorValues: Array[String] = wordAndVectorValues.slice(1, wordAndVectorValues.length)
        val coefs: Array[Float] = vectorValues.map(_.toFloat)
        preWord2Vec.put(word2Meta(word).index.toFloat, coefs)
      }
    }
    log.info(s"Found ${preWord2Vec.size} word vectors.")
    preWord2Vec.toMap
  }

  def buildWord2Vec(word2Meta: Map[String, WordMeta], embeddings: Seq[String]): Map[Float, Array[Float]] = {
    log.info("Indexing word vectors.")
    val preWord2Vec = MMap[Float, Array[Float]]()
    for (line <- embeddings) {
      val wordAndVectorValues: Array[String] = line.split(" ")
      val word: String = wordAndVectorValues(0)
      if (word2Meta.contains(word)) {
        val vectorValues: Array[String] = wordAndVectorValues.slice(1, wordAndVectorValues.length)
        val coefs: Array[Float] = vectorValues.map(_.toFloat)
        preWord2Vec.put(word2Meta(word).index.toFloat, coefs)
      }
    }
    log.info(s"Found ${preWord2Vec.size} word vectors.")
    preWord2Vec.toMap
  }

  /**
    * Load the training data from the given baseDir
    *
    * @return An array of sample
    */
  private def loadRawData(): (Seq[(String, Int)], Map[String, Int]) = {
    val texts = ArrayBuffer[String]()
    val labels = ArrayBuffer[Int]()
    // category is a string name, label is it's index
    val categoryToLabel = new util.HashMap[String, Int]()
    val categoryPathList = new File(textDataDir).listFiles().filter(_.isDirectory).toList.sorted

    categoryPathList.foreach { categoryPath =>
      val labelId: Int = categoryToLabel.size() + 1 // one-base index
      categoryToLabel.put(categoryPath.getName, labelId)
      val textFiles = categoryPath.listFiles()
        .filter(_.isFile).filter(_.getName.forall(Character.isDigit)).sorted
      textFiles.foreach { file =>
        val source = Source.fromFile(file, "ISO-8859-1")
        val text = try source.getLines().toList.mkString("\n") finally source.close()
        texts.append(text)
        labels.append(labelId)
      }
    }
    this.classNum = labels.toSet.size
    log.info(s"Found ${texts.length} texts.")
    log.info(s"Found $classNum classes")

    (texts.zip(labels), categoryToLabel.asScala.toMap)
  }

  /**
    * Go through the whole data set to gather some meta info for the tokens.
    * Tokens would be discarded if the frequency ranking is less then maxWordsNum
    */
  def analyzeTexts(dataRdd: RDD[(String, Float)], gloveEmbeddingsFile: String)
  : (Map[String, WordMeta], Map[Float, Array[Float]]) = {
    val tokens: RDD[String] = dataRdd.flatMap { case (text: String, label: Float) =>
      SimpleTokenizer.toTokens(text)
    }
    val tokensCount: RDD[(String, Int)] = tokens.map(word => (word, 1)).reduceByKey(_ + _)
    // Remove the top 10 words roughly, you might want to fine tuning this.
    val frequencies: Array[(String, Int)] = tokensCount
      .sortBy(-_._2).collect().slice(10, param.maxWordsNum)

    val indexes: Seq[Int] = Range(1, frequencies.length)
    val word2Meta: Map[String, WordMeta] = frequencies.zip(indexes).map { (item: ((String, Int), Int)) =>
      (item._1._1 /*word*/ , WordMeta(item._1._2 /*count*/ , item._2 /*index*/))
    }.toMap
    (word2Meta, buildWord2Vec(word2Meta, gloveEmbeddingsFile))
  }

  def buildWord2Meta(dataRdd: RDD[(String)]): Map[String, WordMeta] = {
    val tokens: RDD[String] = dataRdd.flatMap { text: String =>
      SimpleTokenizer.toTokens(text)
    }
    val tokensCount: RDD[(String, Int)] = tokens.map(word => (word, 1)).reduceByKey(_ + _)
    // Remove the top 10 words roughly, you might want to fine tuning this.
    val frequencies: Array[(String, Int)] = tokensCount
      .sortBy(-_._2).collect().slice(10, param.maxWordsNum)

    val indexes: Seq[Int] = Range(1, frequencies.length)
    val word2Meta: Map[String, WordMeta] = frequencies.zip(indexes).map { (item: ((String, Int), Int)) =>
      (item._1._1 /*word*/ , WordMeta(item._1._2 /*count*/ , item._2 /*index*/))
    }.toMap
    word2Meta
  }

  // TODO: Replace SpatialConv and SpatialMaxPolling with 1D implementation
  /**
    * Return a text classification model with the specific num of
    * class
    */
  def buildModel(classNum: Int): Sequential[Float] = {
    val model = Sequential[Float]()

    model.add(Reshape(Array(param.embeddingDim, 1, param.maxSequenceLength)))

    model.add(SpatialConvolution(param.embeddingDim, 128, 5, 1))
    model.add(ReLU())

    model.add(SpatialMaxPooling(5, 1, 5, 1))

    model.add(SpatialConvolution(128, 128, 5, 1))
    model.add(ReLU())

    model.add(SpatialMaxPooling(5, 1, 5, 1))

    model.add(SpatialConvolution(128, 128, 5, 1))
    model.add(ReLU())

    model.add(SpatialMaxPooling(35, 1, 35, 1))

    model.add(Reshape(Array(128)))
    model.add(Linear(128, 100))
    model.add(Linear(100, classNum))
    model.add(LogSoftMax())
    model
  }

}


abstract class IeAbstractTextClassificationParams extends Serializable {
  def baseDir: String = "./"

  def modelFile: String = "./"

  def maxSequenceLength: Int = 1000

  def maxWordsNum: Int = 20000

  def trainingSplit: Double = 0.8

  def batchSize: Int = 128

  def embeddingDim: Int = 100

  def partitionNum: Int = 4

  def epochNum: Int = 1
}


/**
  * @param baseDir           The root directory which containing the training and embedding data
  * @param maxSequenceLength number of the tokens
  * @param maxWordsNum       maximum word to be included
  * @param trainingSplit     percentage of the training data
  * @param batchSize         size of the mini-batch
  * @param embeddingDim      size of the embedding vector
  */
case class IeTextClassificationParams(override val baseDir: String = "./",
                                    override val modelFile: String = "./",
                                    override val maxSequenceLength: Int = 1000,
                                    override val maxWordsNum: Int = 20000,
                                    override val trainingSplit: Double = 0.8,
                                    override val batchSize: Int = 128,
                                    override val embeddingDim: Int = 100,
                                    override val partitionNum: Int = 4,
                                    override val epochNum: Int = 1)
  extends IeAbstractTextClassificationParams