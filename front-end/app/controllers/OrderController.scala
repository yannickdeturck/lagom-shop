package controllers

import javax.inject.{Inject, Singleton}

import models.{Item, Order}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent, Controller, Request}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * @author Yannick De Turck
  */
@Singleton
class OrderController @Inject()(val messagesApi: MessagesApi)(implicit context: ExecutionContext, ws: WSClient)
  extends Controller with I18nSupport {

  val orderForm: Form[Order] = Form(
    mapping(
      "id" -> ignored(None: Option[String]),
      "itemId" -> nonEmptyText(maxLength = 36, minLength = 36),
      "amount" -> number(0, 100),
      "customer" -> nonEmptyText(maxLength = 48)
    )(Order.apply)(Order.unapply)
  )

  def index = Action.async { implicit request =>
    val getOrders = ws.url("http://" + request.host + "/api/orders")
      .withHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    implicit val orderReads = Json.reads[Order]
    getOrders.get().map {
      response => response.json.validate[List[Order]] match {
        case JsError(errors) =>
          Logger.error("Error while trying to treat getOrders response")
          InternalServerError(errors.toString())
        case JsSuccess(orders, _) =>
          Ok(views.html.orders.index(orders))
      }
    }
  }

  def newOrder = Action { implicit request =>
    Ok(views.html.orders.create(orderForm, getItems(request)))
  }

  def createOrder = Action.async { implicit request =>
    orderForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(views.html.orders.create(errors, getItems(request)))), {
        order =>
          val createOrder = ws.url("http://" + request.host + "/api/orders")
            .withHeaders("Accept" -> "application/json")
            .withRequestTimeout(10000.millis)
          implicit val orderReads = Json.format[Order]
          val response = createOrder.post(Json.toJson(order))
          response.map {
            r =>
              val id = (r.json \ "id").as[String]
              Redirect(routes.OrderController.editOrder(id))
          } recover {
            case t: Throwable =>
              Logger.error("Error while trying to treat createOrder response")
              InternalServerError(t.getMessage)
          }
      }
    )
  }

  def editOrder(id: String) = Action.async { implicit request =>
    val getOrder = ws.url(s"http://${request.host}/api/orders/$id")
    getOrder.get().map {
      response =>
        implicit val orderReads = Json.format[Order]
        response.json.validate[Order] match {
          case JsError(errors) =>
            Redirect(routes.OrderController.index())
          case JsSuccess(order, _) =>
            Ok(views.html.orders.edit(orderForm.fill(order), getItems(request)))
        }
    }
  }

  // TODO move to a service
  def getItems(request: Request[AnyContent]): List[Item] = {
    val getItems = ws.url("http://" + request.host + "/api/items")
      .withHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    implicit val itemReads = Json.reads[Item]
    val items = getItems.get().map {
      response => response.json.validate[List[Item]] match {
        case JsError(errors) =>
          Logger.error("Error while trying to treat getItems response")
          List.empty[Item]
        case JsSuccess(items, _) =>
          items
      }
    }
    Await.result(items, 10.seconds)
  }

}
