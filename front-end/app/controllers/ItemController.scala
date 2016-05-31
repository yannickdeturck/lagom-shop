package controllers

import javax.inject._

import models.Item
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * @author Yannick De Turck
  */
@Singleton
class ItemController @Inject()(val messagesApi: MessagesApi)(implicit context: ExecutionContext, ws: WSClient)
  extends Controller with I18nSupport {
  val itemForm: Form[Item] = Form(
    mapping(
      "id" -> ignored(None: Option[String]),
      "name" -> nonEmptyText(maxLength=28),
      "price" -> bigDecimal(8, 2).verifying("Price must be a positive value", price => price.signum > 0)
    )(Item.apply)(Item.unapply)
  )

  def index = Action.async { implicit request =>
    val getItems = ws.url("http://" + request.host + "/api/items")
      .withHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
    implicit val itemReads = Json.reads[Item]
    getItems.get().map {
      response => response.json.validate[List[Item]] match {
        case JsError(errors) =>
          Logger.error("Error while trying to treat getItems response")
          InternalServerError(errors.toString())
        case JsSuccess(items, _) =>
          Ok(views.html.items.index(items))
      }
    }
  }

  def newItem = Action { implicit request =>
    Ok(views.html.items.create(itemForm))
  }

  def createItem = Action.async { implicit request =>
    itemForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(views.html.items.create(errors))),{
        item =>
          val createItem = ws.url("http://" + request.host + "/api/items")
            .withHeaders("Accept" -> "application/json")
            .withRequestTimeout(10000.millis)
          implicit val itemReads = Json.format[Item]
          val response = createItem.post(Json.toJson(item))
          response.map{
            r =>
              val id = (r.json \ "id").as[String]
              Redirect(routes.ItemController.editItem(id))
          } recover {
            case t: Throwable =>
              Logger.error("Error while trying to treat createItem response")
              InternalServerError(t.getMessage)
          }
      }
    )
  }

  def editItem(id: String) = Action.async { implicit request =>
    val getItem = ws.url(s"http://${request.host}/api/items/$id")
    getItem.get().map {
      response =>
        implicit val itemReads = Json.format[Item]
        response.json.validate[Item] match {
          case JsError(errors) =>
            Redirect(routes.ItemController.index())
          case JsSuccess(item, _) =>
            Ok(views.html.items.edit(itemForm.fill(item)))
        }
    }
  }
}
