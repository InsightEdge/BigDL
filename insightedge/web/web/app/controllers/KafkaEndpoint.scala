//   scalastyle:off

package controllers

import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger

import com.gigaspaces.document.DocumentProperties
import com.gigaspaces.metadata.{SpaceTypeDescriptor, SpaceTypeDescriptorBuilder}
import com.j_spaces.core.LeaseContext
import kafka.producer.{KeyedMessage, Producer, ProducerConfig}
import model.web.Speech
import org.openspaces.core.GigaSpaceConfigurer
import org.openspaces.core.space.SpaceProxyConfigurer
import play.api.libs.json._
import io.insightedge.bigdl.model.InProcessCall

import play.api.mvc._

object KafkaEndpoint extends Controller {

  val counter = new AtomicInteger(0)

  implicit val speechReader = Json.reads[Speech]

  val grid = {
    val spaceConfigurer = new SpaceProxyConfigurer("insightedge-space").lookupGroups("insightedge").lookupLocators("127.0.0.1:4174")
    new GigaSpaceConfigurer(spaceConfigurer).create()
  }

  def submitSpeech = Action(parse.json) { request =>
    parseJson(request) { speech: Speech =>
      val rowId = counter.incrementAndGet()
      val id = writeToSpace(speech.speech)
      println(s"Sending $id + $speech" )
      send(id, speech.toString, "texts")
      Created(rowId.toString)
    }
  }

  def writeToSpace(speech: String): String = {
    val call = InProcessCall(null, speech)
    grid.write(call).getUID
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

  private def send(id: String, message: String, topic: String) = producer.send(new KeyedMessage[String, String](topic, id, message))

}