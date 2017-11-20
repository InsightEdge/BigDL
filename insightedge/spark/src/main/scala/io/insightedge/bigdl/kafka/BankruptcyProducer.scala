//   scalastyle:off

package io.insightedge.bigdl.kafka

import java.util.Properties

import org.apache.kafka.clients.producer._

/**
  * @author Danylo_Hurin.
  */
object BankruptcyProducer extends App {

  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")

  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  val producer = new KafkaProducer[Nothing, String](props)

  val topic = "bankruptcy"
  val sleepMilliseconds = 1000//args(0).toInt

  val data = Array("A","P","N")
  val recordsSize = 6
  val r = util.Random

  while (true) {
    var i = 0
    var values = ""
    while (i < recordsSize) {
      values = values + (if (i == recordsSize - 1) {
        r.nextDouble() * 100 + ""
      } else {
        r.nextDouble() * 100 + ","
      })
      i = i+1
    }
    val record: ProducerRecord[Nothing, String] = new ProducerRecord(topic, values)
    println(s"Sent: $values")
    producer.send(record)

    Thread.sleep(sleepMilliseconds)
  }

  producer.close()

}
