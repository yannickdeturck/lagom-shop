package controllers

import javax.inject.{Inject, Singleton}

import models.Order
import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * @author Yannick De Turck
  */
@Singleton
class OrderController  @Inject()(implicit context: ExecutionContext, ws: WSClient) extends Controller {
  def index = Action.async { implicit request =>
    val getOrders = ws.url("http://" + request.host + "/api/orders")
      .withHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    implicit val orderReads = Json.reads[Order]
    getOrders.get().map {
      response => response.json.validate[List[Order]] match {
        case JsError(errors) =>
          Ok(views.html.orders.index(List.empty[Order])) // TODO return error instead
        case JsSuccess(orders, _) =>
          Ok(views.html.orders.index(orders))
      }
    }

    //    Ok(views.html.items.index(List.empty[Item]))
  }
}
