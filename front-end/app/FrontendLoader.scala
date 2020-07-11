import javax.inject.Inject
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.api.{ServiceAcl, ServiceInfo}
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.softwaremill.macwire._
import controllers.{Assets, HomeController, ItemController, OrderController}
import play.api.ApplicationLoader.Context
import play.api.i18n.I18nComponents
import play.api.libs.ws.ahc.AhcWSComponents
import play.api._
import router.Routes

import scala.collection.immutable
import scala.concurrent.ExecutionContext

abstract class Frontend @Inject()(context: Context) extends BuiltInComponentsFromContext(context)
  with I18nComponents
  with AhcWSComponents
  with LagomServiceClientComponents {

  override lazy val serviceInfo: ServiceInfo = ServiceInfo(
    "frontend",
    Map(
      "frontend" -> immutable.Seq(ServiceAcl.forPathRegex("(?!/api/).*"))
    )
  )
  override implicit lazy val executionContext: ExecutionContext = actorSystem.dispatcher
  override lazy val router = {
    val prefix = "/"
    wire[Routes]
  }

  lazy val homeController = wire[HomeController]
  lazy val itemController = wire[ItemController]
  lazy val orderController = wire[OrderController]
  lazy val assets = wire[Assets]
}

class FrontendLoader extends ApplicationLoader {
  override def load(context: Context): Application = context.environment.mode match {
    case Mode.Dev =>
      new Frontend(context) with LagomDevModeComponents {}.application
    case _ =>
      new Frontend(context) {
        override def serviceLocator = NoServiceLocator
      }.application
  }
}