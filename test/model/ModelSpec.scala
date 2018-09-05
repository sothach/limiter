package model

import common.model.PhoneNumber
import limits.model.CreditLimit
import org.scalatest.{FlatSpec, Matchers}

class ModelSpec extends FlatSpec with Matchers {

  "A canonical Phone number strings" should "be encoded as domain objects" in {
    PhoneNumber.parse("+35317277372").exists(_.value == 35317277372L) shouldBe true
  }

  "A valid Phone number strings" should "be encoded as domain objects" in {
    PhoneNumber.parse("0049 172 9182711 ").exists(_.value == 491729182711L) shouldBe true
  }

  "A valid limits value map" should "be encoded as domain object" in {
    val values = Map(
      "Name" -> "Johnson, John", "Address" -> "Voorstraat 32", "Postcode " -> "3122gg",
      "Phone" -> " 020 3849381", "Credit Limit" -> "1000000", "Birthday" -> "19870101")
    val subject = CreditLimit.fromMap(values)
    subject match {
      case Some(limit) =>
        limit.name shouldBe "Johnson, John"
      case None =>
        fail
    }
  }

  "A valid limits value map with a euro date" should "be encoded as domain objects" in {
    val values = Map(
      "Name" -> "Johnson, John", "Address" -> "Voorstraat 32", "Postcode " -> "3122gg",
      "Phone" -> " 020 3849381", "Credit Limit" -> "1000000", "Birthday" -> "01/01/1987")
    val subject = CreditLimit.fromMap(values)
    subject match {
      case Some(limit) =>
        limit.dateOfBirth.toString shouldBe "1987-01-01"
      case None =>
        fail
    }
  }

  "An limits value map with missing columns" should "trigger a precondition violation" in {
    val values = Map(
      "Name" -> "Johnson, John", "Address" -> "Voorstraat 32", "Postcode " -> "3122gg", "Phone" -> " 020 3849381")
    the [IllegalArgumentException] thrownBy {
      CreditLimit.fromMap(values)
    } should have message "requirement failed: map must contain values for List(name, address, postcode, phone, credit limit, birthday)"
  }


  "An limits value map with invalid values" should "fail" in {
    val values = Map(
      "Name" -> "Johnson, John", "Address" -> "Voorstraat 32", "Postcode " -> "3122gg",
      "Phone" -> " 020 3849381", "Credit Limit" -> "1000000", "Birthday" -> "yyyymmdd")
    CreditLimit.fromMap(values) shouldBe None
  }

}
