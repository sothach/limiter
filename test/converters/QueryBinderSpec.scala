package converters

import common.model.ApiKey
import conversions.Binders._
import org.scalatest.{FlatSpec, Matchers}

class QueryBinderSpec extends FlatSpec with Matchers {

  "An ApiKey" should "be unbound as a query parameter" in {
    val key = ApiKey("eabb12404d141ed6e8ee2193688178cb")
    val result = apiKeyBinder.unbind("apiKey", key)
    result shouldBe "apiKey=eabb12404d141ed6e8ee2193688178cb"
  }

  "A valid ApiKey parameter" should "bind" in {
    val result = apiKeyBinder.bind("apiKey", Map("apiKey" -> Seq("eabb12404d141ed6e8ee2193688178cb")))
    result shouldBe Some(Right(ApiKey("eabb12404d141ed6e8ee2193688178cb")))
  }

  "An invalid ApiKey parameter" should "not bind" in {
    val result = apiKeyBinder.bind("apiKey", Map("noKey" -> Seq.empty))
    result.exists(_.isLeft) shouldBe true
  }

}
