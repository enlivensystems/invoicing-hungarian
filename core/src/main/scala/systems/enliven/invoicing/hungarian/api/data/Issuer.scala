package systems.enliven.invoicing.hungarian.api.data

import systems.enliven.invoicing.hungarian.core.requirement.StringRequirement._

import scala.util.matching.Regex

case class Issuer(
  taxNumber: String,
  taxCode: String,
  taxCounty: String,
  communityTaxNumber: String,
  name: String,
  address: Address,
  bankAccountNumber: String) {
  taxNumber.named("taxNumber").nonEmpty.trimmed
  taxCode.named("taxCode")
    .nonEmpty.trimmed.matches(Issuer.taxCodeRegex.regex)
  taxCounty.named("taxCounty")
    .nonEmpty.trimmed.matches(Issuer.taxCountyRegex.regex)
  communityTaxNumber.named("communityTaxNumber")
    .nonEmpty.trimmed.matches(Issuer.communityTaxNumberRegex.regex)
  name.named("name").nonEmpty.trimmed
  bankAccountNumber.named("bankAccountNumber").nonEmpty.trimmed
}

object Issuer {
  final val taxCodeRegex: Regex = """[1-5]""".r
  final val taxCountyRegex: Regex = """[0-9]{2}""".r
  final val communityTaxNumberRegex: Regex = """[A-Z]{2}[0-9A-Z]{2,13}""".r
}
