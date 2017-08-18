//   scalastyle:off

package controllers

import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger

import kafka.producer.{KeyedMessage, Producer, ProducerConfig}
import model.web.Speech
import play.api.libs.json._
import play.api.mvc._

object KafkaEndpoint extends Controller {

  val counter = new AtomicInteger(0)

  implicit val flightsReader = Json.reads[Speech]
//  implicit val submittedFlightsReader = Json.reads[SubmittedFlight]

  def submitFlight = Action(parse.json) { request =>
    parseJson(request) { speech: Speech =>
      println(speech)
      println(speech)
      println(speech)
      println(speech)
      println(speech)
      println(speech)
      println(speech)
      println(speech)
      println(speech)
      println(speech)
      val rowId = counter.incrementAndGet()
      send(speech.toString(), "texts")
      Created(rowId.toString)
    }
  }

  private def parseJson[R](request: Request[JsValue])(block: R => Result)(implicit reads: Reads[R]): Result = {
    request.body.validate[R](reads).fold(
      valid = block,
      invalid = e => {
        val error = e.mkString
        BadRequest(error)
      }
    )
  }

  // hardcoded to simplify the demo code
  lazy val kafkaConfig = {
    val props = new Properties()
    props.put("metadata.broker.list", "localhost:9092")
    props.put("serializer.class", "kafka.serializer.StringEncoder")
    props
  }
  lazy val producer = new Producer[String, String](new ProducerConfig(kafkaConfig))

  private def send(message: String, topic: String) = producer.send(new KeyedMessage[String, String](topic, message))

}