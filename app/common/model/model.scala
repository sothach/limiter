package common

package object model {

  case class PhoneNumber(value: Long) extends AnyVal {
    override def toString = s"+$value"
  }
  object PhoneNumber {
    val numPattern = "(\\+|00)?(\\d+)".r
    def parse(value: String): Option[PhoneNumber] = value.replaceAll("[\\s\\.-]","") match {
      case numPattern(_,number) =>
        Some(PhoneNumber(number.toLong))
      case _ =>
        None
    }
  }

  case class ApiKey private (value: String)

  object ApiKey {
    val pattern = "([a-f0-9]{32})".r
    def apply(value: String): ApiKey =
      value.trim.toLowerCase match {
        case pattern(key) =>
          new ApiKey(key)
        case _ =>
          new ApiKey("")
      }
  }

}
