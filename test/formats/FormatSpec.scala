package formats

import org.scalatest.{FlatSpec, Matchers}

import scala.annotation.tailrec
import scala.io.Source

class FormatSpec extends FlatSpec with Matchers {

  @tailrec
  final def slicer(s: String, size: Seq[Int], acc: Seq[String] = Seq.empty): Seq[String] = {
    if(s.length > size.head) {
      slicer(s.drop(size.head), size.drop(1), acc :+ s.take(size.head))
    } else {
      acc :+ s
    }
  }

/*  def niceFeedbackReadResource(resource: String): List[String] =
    Try(Source.fromResource(resource).getLines.toList)
      .recover(throw new FileNotFoundException(resource))*/

  trait Pic
  case class NumericPic(n: Int) extends Pic
  case class AlphaNumPic(n: Int) extends Pic
  trait Element {
    def level: Int
  }
  case class Field(level: Int, name: String, pic: Pic) extends Element
  case class Struct(level: Int, elements: Seq[Field]) extends Element

  def normalize(name: String) = name
    .toLowerCase
    .split('-')
    .map(_.capitalize)
    .mkString

  val linex = """\s*(\d{2})\s+([A-Z0-9\(\)\s-]+)\s*\.?""".r
  val namex = """([A-Z-]+)\s+PIC\s+([X9]+|[X9]\(\d+\))""".r
  val literalPix = """([X9]+)""".r
  val occursPix = """([X9])\((\d+)\)""".r
  val nPix = """\((\d+)\)""".r

  "A valid format file" should "be parsed" in {

    val bufferedSource = Source.fromResource("copybook1.cbl")
    for (line <- bufferedSource.getLines) {
      process(line)
    }

    bufferedSource.close
  }

/*  @tailrec
  final def parser(lines: Seq[String], acc: Struct): Struct = {
    if(s.length > size.head) {
      parser(s.drop(size.head), size.drop(1), acc :+ s.take(size.head))
    } else {
      acc :+ s
    }
  }*/

  private def process(line: String) = {
    line.toUpperCase match {
      case linex(level, statement) =>
        statement match {
          case namex(name, picz) =>
            val (ftype, width) = picz match {
              case literalPix(p) =>
                (p.head, p.length)
              case occursPix(t, w) =>
                (t.head, w.toInt)
            }
            val pic = ftype match {
              case 'X' => AlphaNumPic(width)
              case '9' => NumericPic(width)
            }
            val field = Field(level.toInt, normalize(name), pic)
            println(s"$field")
          case _ =>
            println(s"error in: '$statement'")
        }
      case _ =>
        println(s"error in: '$line'")
    }
  }
}
