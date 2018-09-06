package controllers

import common.model.ApiKey
import javax.inject.{Inject, Singleton}
import limits.LimitsService
import play.api.Logger
import play.api.mvc._

import scala.concurrent.Future

@Singleton
class ApiController @Inject()(limitsService: LimitsService,
                              components: ControllerComponents) 
                                extends AbstractController(components) {

  implicit val ec = components.executionContext
  val logger = Logger(this.getClass)

  private val expectKey = ApiKey("eabb12404d141ed6e8ee2193688178cb")
  logger.info("ApiController started")

  def index = Action.async {
    Future.successful(Ok)
  }

  def limits(apiKey: ApiKey) = Action.async(fromFile) { request =>
    apiKey match {
      case key if key == expectKey =>
        limitsService.renderFromSource(request.body) map {
          case response if response.exists(_.isRight) =>
            val limits = response.filter(_.isRight).map(_.right.get)
            Ok(views.html.results(limitsService.headings,limits))
          case response: Seq[Either[String, _]] if response.exists(_.isLeft) =>
            val errors = response.filter(_.isLeft).map(_.left.get).mkString(";")
            logger.debug(s"Upload errors: $errors")
            BadRequest(errors)
          case _ =>
            BadRequest
        }
      case _ =>
        logger.warn(s"Missing or invalid API key in $request")
        Future.successful(Unauthorized("Missing or invalid API key"))
    }
  }

  def fromFile = BodyParser { request =>
    limitsService.dataSource(
      request.charset.getOrElse("UTF-8"),
      request.contentType.getOrElse("text/plain"))
  }

}
