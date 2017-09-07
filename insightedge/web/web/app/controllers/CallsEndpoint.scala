//   scalastyle:off

package controllers

import java.util.concurrent.TimeUnit

import com.j_spaces.core.client.SQLQuery
import model.grid.ModelStats
import org.openspaces.core.GigaSpaceConfigurer
import org.openspaces.core.space.SpaceProxyConfigurer
import play.api.libs.json._
import play.api.mvc._
import io.insightedge.bigdl.model.{CallSession, InProcessCall, TrainedModelStats}

object CallsEndpoint extends Controller {

  implicit val orderStatusWrites = new Writes[CallSession] {
    override def writes(f: CallSession): JsValue = JsString(f.getClass.getSimpleName)
  }

  val callSessionWriter = Json.writes[CallSession]
  val callSessionListWriter = Writes.list[CallSession](callSessionWriter)

  val inProcessCallWriter = Json.writes[InProcessCall]
  val inProcessCallListWriter = Writes.list[InProcessCall](inProcessCallWriter)

  val modelStatsWriter = Json.writes[ModelStats]

  val grid = {
    val spaceConfigurer = new SpaceProxyConfigurer("insightedge-space").lookupGroups("insightedge").lookupLocators("127.0.0.1:4174")
    new GigaSpaceConfigurer(spaceConfigurer).create()
  }

  def getLastCallSessions(streamedRowId: String) = Action { implicit request =>
    val query = new SQLQuery[CallSession](classOf[CallSession], "counter > ? ORDER BY counter ASC")
    query.setParameter(1, streamedRowId.toLong)
    val callSessions = grid.readMultiple(query)
    Ok(Json.toJson(callSessions.toList)(callSessionListWriter))
  }

  def getInprocessCalls() = Action { implicit request =>
    val query = new SQLQuery[InProcessCall](classOf[InProcessCall], "ORDER BY id ASC")
    val calls = grid.readMultiple(query)
    Ok(Json.toJson(calls.toList)(inProcessCallListWriter))
  }

  def getModelStatistic() = Action { implicit request =>
    val query = new SQLQuery[TrainedModelStats](classOf[TrainedModelStats], "ORDER BY id ASC")
    val stats = grid.read(query)
    Ok(Json.toJson(toModelStats(stats))(modelStatsWriter))
  }

  def toModelStats(stats: TrainedModelStats): ModelStats = {
    val time = stats.timeInMilliseconds
    val mins = TimeUnit.MILLISECONDS.toMinutes(time)
    val accuracy = stats.accuracy
    ModelStats(
      s"$mins min",
      accuracy.toString
    )
  }

}