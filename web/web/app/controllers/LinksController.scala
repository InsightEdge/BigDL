//   scalastyle:off


package controllers

import play.api.mvc.{Action, Controller, Result}

/**
  * @author Danylo_Hurin.
  */
object LinksController extends Controller {

  def  tensorFlow() = Action { implicit request =>
    // or pass as a redirect() arg
    Redirect("http://localhost:6006/", 302)
  }

  def  sparkJobs() = Action { implicit request =>
    // or pass as a redirect() arg
    Redirect("http://localhost:4040/jobs/", 302)
  }

  def  ieWebUi() = Action { implicit request =>
    // or pass as a redirect() arg
    Redirect("http://localhost:8099", 302)
  }

}
