package routing

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.Await
import scala.concurrent.duration._

class RouteSpec extends PlaySpec with ScalaFutures with GuiceOneAppPerSuite {
  val system = ActorSystem.create("test-actor-system")
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system))(system)

  override def fakeApplication() = new GuiceApplicationBuilder()
    .loadConfig(Configuration(ConfigFactory.load("application.conf")))
    .build()

  "GET /" should {"invoke the index" in {
      val request = FakeRequest("GET", "/")
        .withHeaders(FakeHeaders(Map("Host" -> "localhost").toSeq))

      route(app, request).map(status) mustBe Some(OK)
    }
  }

  "valid CSV limits posted to the /api/limits endpoint" should {
    "be rendered as HTML" in {
      val file = Seq(
        "Name,Address,Postcode,Phone,Credit Limit,Birthday",
        """"Johnson, John",Voorstraat 32,3122gg,020 3849381,10000,01/01/1987""")
      val request = FakeRequest("POST", s"/api/limits?apiKey=$apiKey")
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Content-Type" -> "text/csv").toSeq))
        .withBody(file.mkString("\n"))
      route(app, request).map(status) mustBe Some(OK)

      route(app, request) foreach { future =>
        val response = Await.result(future, 10 seconds)
        response.header.status mustBe OK
        response.body.contentType mustBe Some("text/html; charset=utf-8")
        response.body.isKnownEmpty mustBe false
        val result = Await.result(response.body.consumeData, 2 seconds)
        val html = scala.xml.XML.loadString(result.utf8String)
        (html \\ "td").map(_.text) mustBe List("Johnson, John", "Voorstraat 32", "3122gg", "+203849381", "10000", "1987-01-01")
      }
    }
  }

  "valid Fixed limits posted to the /api/limits endpoint" should {
    "be rendered as HTML" in {
      val file = Seq(
        "Name            Address               Postcode Phone         Credit Limit Birthday",
        "Johnson, Jane   Voorstraat 33         3322gg   020 3849999             10 19990909")
      val request = FakeRequest("POST", s"/api/limits?apiKey=$apiKey")
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Content-Type" -> "text/plain").toSeq))
        .withBody(file.mkString("\n"))
      route(app, request) foreach { future =>
        val response = Await.result(future, 10 seconds)
        response.header.status mustBe OK
        response.body.contentType mustBe Some("text/html; charset=utf-8")
        response.body.isKnownEmpty mustBe false
        val result = Await.result(response.body.consumeData, 2 seconds)
        val html = scala.xml.XML.loadString(result.utf8String)
        (html \\ "td").map(_.text) mustBe List("Johnson, Jane", "Voorstraat 33", "3322gg", "+203849999", "10", "1999-09-09")
      }
    }
  }

  "An invalid limits record posted to the /api/limits endpoint" should {
    "respond with an error" in {
      val file = Seq(
        "Name            Address               Postcode Phone         Credit Limit",
        "Johnson, John   Voorstraat 32         3122gg   020 3849381        1000000")
      val request = FakeRequest("POST", s"/api/limits?apiKey=$apiKey")
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Content-Type" -> "text/plain").toSeq))
        .withBody(file.mkString("\n"))
      route(app, request) foreach { future =>
        val response = Await.result(future, 10 seconds)
        response.header.status mustBe BAD_REQUEST
      }
    }
  }

  "A limits record with invalid values posted to the /api/limits endpoint" should {
    "respond with an error" in {
      val file = Seq(
        "Name            Address               Postcode Phone         Credit Limit Birthday",
        "Johnson, Jane   Voorstraat 33         3322gg   ABC 3849999             10 19990909")
      val request = FakeRequest("POST", s"/api/limits?apiKey=$apiKey")
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Content-Type" -> "text/plain").toSeq))
        .withBody(file.mkString("\n"))
      route(app, request) foreach { future =>
        val response = Await.result(future, 10 seconds)
        response.header.status mustBe BAD_REQUEST
        val result = Await.result(response.body.consumeData, 2 seconds)
        result.utf8String mustBe "Unable to build CreditLimit from values"
      }
    }
  }

  "A posting to the /api/limits endpoint with an invalid api key" should {
    "respond with an error" in {
      val file = Seq(
        "Name            Address               Postcode Phone         Credit Limit Birthday",
        "Johnson, Jane   Voorstraat 33         3322gg   ABC 3849999             10 19990909")
      val request = FakeRequest("POST", s"/api/limits?apiKey=12345")
        .withHeaders(FakeHeaders(Map(
          "Host" -> "localhost",
          "Content-Type" -> "text/plain").toSeq))
        .withBody(file.mkString("\n"))
      route(app, request) foreach { future =>
        val response = Await.result(future, 10 seconds)
        response.header.status mustBe UNAUTHORIZED
        val result = Await.result(response.body.consumeData, 2 seconds)
        result.utf8String mustBe "Missing or invalid API key"
      }
    }
  }

  val apiKey = "eabb12404d141ed6e8ee2193688178cb"

}
