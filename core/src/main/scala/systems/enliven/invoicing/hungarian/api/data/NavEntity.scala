package systems.enliven.invoicing.hungarian.api.data

import systems.enliven.invoicing.hungarian.core.Configuration

case class NavEntity(
  credentials: NavEntity.Credentials,
  taxNumber: String
) {
  require(taxNumber.matches("[0-9]{8}"))
}

object NavEntity {

  def create(taxNumber: String, credentials: Credentials): NavEntity =
    NavEntity(
      credentials = credentials,
      taxNumber = taxNumber
    )

  def create(signingKeyOverride: Option[String] = None)(
    implicit configuration: Configuration
  ): NavEntity =
    NavEntity(
      credentials = Credentials(
        signingKey = signingKeyOverride
          .getOrElse(configuration.get[String]("invoicing-hungarian.authentication.signing-key")),
        exchangeKey = configuration.get[String]("invoicing-hungarian.authentication.exchange-key"),
        login = configuration.get[String]("invoicing-hungarian.authentication.login"),
        password = configuration.get[String]("invoicing-hungarian.authentication.password")
      ),
      taxNumber = configuration.get[String]("invoicing-hungarian.entity.tax-number")
    )

  case class Credentials(
    signingKey: String,
    exchangeKey: String,
    login: String,
    password: String) {
    require(signingKey.trim.nonEmpty)
    require(exchangeKey.trim.nonEmpty)
    require(login.trim.nonEmpty)
    require(password.trim.nonEmpty)
  }

}
