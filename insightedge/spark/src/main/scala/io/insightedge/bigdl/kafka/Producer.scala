//   scalastyle:off

package io.insightedge.bigdl.kafka

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.Properties

import org.apache.kafka.clients.producer._

/**
  * @author Danylo_Hurin.
  */
object Producer extends App {


  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")

  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  val producer = new KafkaProducer[String, String](props)

  val topic = "texts"
  val dataDir = args(0)
  val filesPerBatch = args(1).toInt
  val sleepSeconds = args(2).toInt
  val files = recursiveListFiles(new File(dataDir)).toSet

  var i = 0
  var count = 0
  while (i < files.size) {
    val batch = files.slice(i, i + filesPerBatch)
    for (file <- batch) {
      val text = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath)))
      val record = new ProducerRecord(topic, file.getName, text)
      producer.send(record)
      count = count + 1
    }
    println(s"Total sent files count: $count")
//    producer.send(new ProducerRecord(TOPIC, "-----------", "-----------"))

    Thread.sleep(sleepSeconds * 1000)
    if ((i < files.size) && (i + filesPerBatch > files.size))
      i = files.size
    else
      i = i + filesPerBatch
  }

//  producer.send(new ProducerRecord(TOPIC, "", s"All ${files.size.toString} Sent ${count.toString}"))

  producer.close()

  def recursiveListFiles(f: File): Array[File] = {
    val these: Array[File] = f.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }

}
