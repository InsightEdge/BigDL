//   scalastyle:off


package controllers

import play.api.mvc.{Action, Controller}

/**
  * @author Danylo_Hurin.
  */
object LinksController extends Controller {

  val IP_ADDRESS="http://localhost"

  def  tensorFlow() = Action { implicit request =>
    // or pass as a redirect() arg
    Redirect(s"$IP_ADDRESS:6006/", 302)
  }

  def  sparkJobs() = Action { implicit request =>
    // or pass as a redirect() arg
    Redirect(s"$IP_ADDRESS:4040/jobs/", 302)
  }

  def  ieWebUi() = Action { implicit request =>
    // or pass as a redirect() arg
    Redirect(s"$IP_ADDRESS:8099", 302)
  }

}
