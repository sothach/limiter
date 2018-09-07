package limits

import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Framing, Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.util.{ByteString, Timeout}
import javax.inject.{Inject, Singleton}
import limits.model._
import play.api.libs.streams.Accumulator
import play.api.{Configuration, Environment, Logger}

import scala.annotation.tailrec

@Singleton
class LimitsService @Inject()(implicit system: ActorSystem,
                              configuration: Configuration,
                              environment: Environment) {

  implicit val ec = system.dispatcher
  val logger = Logger(this.getClass)

  val decider: Supervision.Decider = {
    case e: IllegalArgumentException =>
      logger.warn(s"Assertion failure: ${e.getMessage}")
      Supervision.Resume
  }
  val materializerSettings = ActorMaterializerSettings(system).withSupervisionStrategy(decider)
  implicit val materializer = ActorMaterializer(materializerSettings)(system)
  implicit val timeout = Timeout(1, TimeUnit.SECONDS)

  val fixedColumnsWidths = configuration.get[Seq[Int]]("formats.fixed.columnsWidths")

  val headings = configuration.get[Seq[String]]("formats.headings").toList

  def dataSource(charset: String, contentType: String) = {
    def splitter(s: String) = contentType match {
      case "text/csv" =>
        val csvRegex = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)"
        s.split(csvRegex).toSeq
      case _ =>
        @tailrec
        def slicer(s: String, size: Seq[Int], acc: Seq[String] = Seq.empty): Seq[String] = {
          if(s.length > size.head) {
            slicer(s.drop(size.head), size.drop(1), acc :+ s.take(size.head))
          } else {
            acc :+ s
          }
        }
        slicer(s,fixedColumnsWidths)
    }

    val rowSplitter = Flow[ByteString]
      .via(Framing.delimiter(ByteString("\n"), Int.MaxValue, allowTruncation = true))
      .map(_.decodeString(charset).trim)
      .map(splitter)

    Accumulator.source[ByteString].map(_.via(rowSplitter)).map(Right.apply)
  }

  val filter = Flow[Seq[String]]
    .collect { case element if element.nonEmpty => element}

  val mapify: Flow[Seq[String], Map[String, String], NotUsed] =
    Flow[Seq[String]].prefixAndTail(1).flatMapConcat {
      case (headers, rows) =>
        val header = headers.headOption
          .getOrElse(Seq.empty).map(_.toLowerCase)
        rows.map(header.zip(_).toMap)
    }

  val validate = Flow[Map[String, String]] map { values =>
    CreditLimit.fromMap(values).toRight("Unable to build CreditLimit from values")
  }

  def renderFromSource(source: Source[Seq[String],_]) =
    source.async via filter via mapify via validate runWith Sink.seq

}
