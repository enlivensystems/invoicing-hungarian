package systems.enliven.invoicing.hungarian.api

import systems.enliven.invoicing.hungarian.core.Configuration

case class Data(auth: Data.Authentication,
                entity: Data.Entity,
                request: Data.Request,
                software: Data.Software)

object Data {
  def apply(signingKeyOverride: Option[String] = None)(
    implicit configuration: Configuration): Data = {
    val auth = Authentication(
      signingKeyOverride
        .getOrElse(configuration.get[String]("invoicing-hungarian.authentication.signing-key")),
      configuration.get[String]("invoicing-hungarian.authentication.exchange-key"),
      configuration.get[String]("invoicing-hungarian.authentication.login"),
      configuration.get[String]("invoicing-hungarian.authentication.password")
    )
    val entity = Entity(configuration.get[String]("invoicing-hungarian.entity.tax-number"))
    val request = Request(configuration.get[String]("invoicing-hungarian.request.base"))
    val software = Software(
      configuration.get[String]("invoicing-hungarian.software.identifier"),
      configuration.get[String]("invoicing-hungarian.software.name"),
      configuration.get[String]("invoicing-hungarian.software.version"),
      Developer(
        configuration.get[String]("invoicing-hungarian.software.developer.name"),
        configuration.get[String]("invoicing-hungarian.software.developer.contact"),
        configuration.getOption[String]("invoicing-hungarian.software.developer.country-code"),
        configuration.getOption[String]("invoicing-hungarian.software.developer.tax-number")
      )
    )

    new Data(auth, entity, request, software)
  }

  case class Authentication(signingKey: String,
                            exchangeKey: String,
                            login: String,
                            password: String){

  }
  case class Entity(taxNumber: String) {
    require(taxNumber.matches("[0-9]{8}"))
  }
  case class Request(base: String)
  case class Software(id: String,
                      name: String,
                      version: String,
                      developer: Developer) {
    require(id.matches("[0-9A-Z]{18}"))
    require(version.matches(".*[^\\s].*"))
  }
  case class Developer(name: String,
                       contact: String,
                       countryCode: Option[String],
                       taxNumber: Option[String]) {
    require(name.matches(".*[^\\s].*"))
    require(contact.matches(".*[^\\s].*"))
    require(countryCode.forall(_.matches("[A-Z]{2}")))
    require(taxNumber.forall(_.matches(".*[^\\s].*")))
  }
}
