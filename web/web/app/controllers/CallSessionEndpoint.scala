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


object CallSessionEndpoint extends Controller {

  val STREAMED = "1"
  val SUBMITTED = "0"

  implicit val orderStatusWrites = new Writes[CallSession] {
    override def writes(f: CallSession): JsValue = JsString(f.getClass.getSimpleName)
  }

  val callSessionWriter = Json.writes[CallSession]
  val callSessionListWriter = Writes.list[CallSession](callSessionWriter)

  val grid = {
    val spaceConfigurer = new SpaceProxyConfigurer("insightedge-space").lookupGroups("insightedge").lookupLocators("127.0.0.1:4174")
    new GigaSpaceConfigurer(spaceConfigurer).create()
  }

  def getLastCallSessions(streamedRowId: String) = Action { implicit request =>
    val query = new SQLQuery[SpaceDocument]("io.insightedge.bigdl.model.CallSession", "counter > ? ORDER BY counter ASC", QueryResultType.DOCUMENT)
    query.setParameter(1, streamedRowId.toLong)
    val callSessions = grid.readMultiple(query)
    Ok(Json.toJson(callSessions.map(toCallSession).toList)(callSessionListWriter))
  }

  def toCallSession(sd: SpaceDocument): CallSession = {
    CallSession(
      sd.getProperty[String]("id"),
      sd.getProperty[String]("category"),
      sd.getProperty[String]("agentId"),
      sd.getProperty[String]("text"),
      sd.getProperty[Long]("counter")
    )
  }

}