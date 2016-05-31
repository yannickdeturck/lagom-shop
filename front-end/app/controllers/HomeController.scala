package controllers

import javax.inject._

import play.api.mvc._

/**
  * @author Yannick De Turck
  */
@Singleton
class HomeController @Inject() extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

}
