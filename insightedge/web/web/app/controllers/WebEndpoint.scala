//   scalastyle:off

package controllers

import play.api.mvc._

object WebEndpoint extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.callSessions(null))
  }

}