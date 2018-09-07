package limits

import java.time.LocalDate

import common.model.PhoneNumber

package object model {

  private def dateParser(dateString: String) = {
    val euroFormat = """(\d{2})/(\d{2})/(\d{4})""".r
    val fixedFormat = """(\d{4})(\d{2})(\d{2})""".r
    dateString match {
      case fixedFormat(year,month,day) =>
        Some(LocalDate.of(year.toInt,month.toInt,day.toInt))
      case other =>
        other match {
          case euroFormat(day,month,year) =>
            Some(LocalDate.of(year.toInt,month.toInt,day.toInt))
          case _ =>
            None
        }
    }
  }

  implicit class MapClean(values: Map[String,String]) {
    def normalized = values.map { case (key, value) =>
      val nkey = key.trim.toLowerCase
      val nval = value.trim.replaceAll("^\"|\"$", "")
      nkey -> nval
    }
  }

  case class CreditLimit(name: String, address: String, postcode: String, phone: PhoneNumber, limit: BigDecimal, dateOfBirth: LocalDate)
  object CreditLimit {
    val columns = Seq("name","address","postcode","phone","credit limit","birthday")

    def fromMap(values: Map[String,String]): Option[CreditLimit] = {
      require(values.size == columns.size, s"${columns.size} columns required")
      val normal = values.normalized
      require(columns.forall (normal.keys.toList.contains), s"map must contain values for $columns")
      (PhoneNumber.parse(normal("phone")), dateParser(normal("birthday"))) match {
        case (Some(phone),Some(dob)) =>
          Some(CreditLimit(
            normal("name"), normal("address"), normal("postcode"), phone, BigDecimal(normal("credit limit")), dob))
        case _ =>
          None
      }
    }
  }

}
