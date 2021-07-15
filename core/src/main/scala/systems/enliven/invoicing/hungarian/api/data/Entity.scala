package systems.enliven.invoicing.hungarian.api.data

import systems.enliven.invoicing.hungarian.core.requirement.StringRequirement._

import scala.util.matching.Regex

case class Entity(
  credentials: Entity.Credentials,
  taxNumber: String
) {
  taxNumber.named("taxNumber")
    .nonEmpty.trimmed.matches(Entity.taxNumberRegex.regex)
}

object Entity {

  final val taxNumberRegex: Regex = """[0-9]{8}""".r

  def create(taxNumber: String, credentials: Credentials): Entity =
    Entity(
      credentials = credentials,
      taxNumber = taxNumber
    )

  case class Credentials(
    signingKey: String,
    exchangeKey: String,
    login: String,
    password: String) {
    signingKey.named("signingKey").nonEmpty.trimmed
    exchangeKey.named("exchangeKey").nonEmpty.trimmed
    login.named("login").nonEmpty.trimmed
    password.named("password").nonEmpty.trimmed
  }

}
