package systems.enliven.invoicing.hungarian.api

import systems.enliven.invoicing.hungarian.core.Configuration

case class Data(request: Data.Request, software: Data.Software)

object Data {

  def apply()(implicit configuration: Configuration): Data = {
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

    new Data(request, software)
  }

  case class Request(base: String)

  case class Software(id: String, name: String, version: String, developer: Developer) {
    require(id.matches("[0-9A-Z]{18}"))
    require(version.matches(".*[^\\s].*"))
  }

  case class Developer(
    name: String,
    contact: String,
    countryCode: Option[String],
    taxNumber: Option[String]) {
    require(name.matches(".*[^\\s].*"))
    require(contact.matches(".*[^\\s].*"))
    require(countryCode.forall(_.matches("[A-Z]{2}")))
    require(taxNumber.forall(_.matches(".*[^\\s].*")))
  }

}
