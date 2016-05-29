package controllers

import javax.inject._

import models.Item
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * @author Yannick De Turck
  */
@Singleton
class ItemController @Inject()(implicit context: ExecutionContext, ws: WSClient) extends Controller {
  def index = Action.async { implicit request =>
    val getItems = ws.url("http://" + request.host + "/api/items")
      .withHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    implicit val itemReads = Json.reads[Item]
    getItems.get().map {
      response => response.json.validate[List[Item]] match {
        case JsError(errors) =>
          Ok(views.html.items.index(List.empty[Item])) // TODO return error instead
        case JsSuccess(items, _) =>
          Ok(views.html.items.index(items))
      }
    }

    //    Ok(views.html.items.index(List.empty[Item]))
  }

  //  def insert = Action(BodyParsers.parse.json) { request =>
  //    val either = request.body.validate[Person]
  //    either.fold(
  //        errors => BadRequest("invalid json person"),
  //    person => {
  //      repository.+=(person)
  //      Ok
  //    }
  //    )
  //  }

}
