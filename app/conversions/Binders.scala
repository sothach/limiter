package conversions

import common.model.ApiKey
import play.api.mvc.QueryStringBindable

object Binders {

  implicit def apiKeyBinder(implicit stringBinder: QueryStringBindable[String]) = new QueryStringBindable[ApiKey] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApiKey]] = {
      val result = stringBinder.bind("apiKey", params) match {
        case Some(v) if v.isRight =>
          v.map(ApiKey(_))
        case _ =>
          Left("Unable to bind ApKey")
      }
      Some(result)
    }

    override def unbind(key: String, apiKey: ApiKey): String = {
      stringBinder.unbind("apiKey", apiKey.value)
    }
  }

}
