//   scalastyle:off

package controllers

import com.gigaspaces.document.SpaceDocument
import com.j_spaces.core.client.SQLQuery
import model.grid.CallSession
import org.openspaces.core.GigaSpaceConfigurer
import org.openspaces.core.space.SpaceProxyConfigurer
import play.api.libs.json._
import play.api.mvc._
import com.gigaspaces.query.QueryResultType


object FlightEndpoint extends Controller {

  val STREAMED = "1"
  val SUBMITTED = "0"

  implicit val orderStatusWrites = new Writes[CallSession] {
    override def writes(f: CallSession): JsValue = JsString(f.getClass.getSimpleName)
  }

  val flightsWriter = Json.writes[CallSession]
  val flightsListWriter = Writes.list[CallSession](flightsWriter)

  val grid = {
    val spaceConfigurer = new SpaceProxyConfigurer("insightedge-space").lookupGroups("insightedge").lookupLocators("127.0.0.1:4174")
    new GigaSpaceConfigurer(spaceConfigurer).create()
  }


  def getLastFlights(streamedRowId: String) = Action { implicit request =>
    val query = new SQLQuery[SpaceDocument]("io.insightedge.bigdl.model.CallSession", "counter > ? ORDER BY counter ASC", QueryResultType.DOCUMENT)
    println("rowid: " + streamedRowId)
    query.setParameter(1, streamedRowId.toLong)
    val categories = grid.readMultiple(query)
    println(categories.length)
    val flights = categories.map(toFlight).toList

    Ok(Json.toJson(flights)(flightsListWriter))
  }

  def toFlight(sd: SpaceDocument): CallSession = {
    CallSession(
      sd.getProperty[String]("id"),
      sd.getProperty[String]("category"),
      sd.getProperty[String]("agentId"),
      sd.getProperty[String]("text"),
      sd.getProperty[Long]("counter")
    )
  }

}