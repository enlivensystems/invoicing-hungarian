package systems.enliven.invoicing.hungarian.api.data

import systems.enliven.invoicing.hungarian.core.requirement.StringRequirement._

case class Entity(
  credentials: Entity.Credentials,
  taxNumber: String) {
  taxNumber.named("taxNumber").matches(Validation.hungarianTaxpayerIDRegex.regex)
}

object Entity {

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
      .matches(Validation.NAVLoginNameRegex.regex)

    password.named("password").nonEmpty.trimmed
  }

}
