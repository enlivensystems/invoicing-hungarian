package systems.enliven.invoicing.hungarian.api.data

import systems.enliven.invoicing.hungarian.core.requirement.StringRequirement._

import scala.util.matching.Regex

case class TaxNumber(
  taxPayerID: String,
  vatCode: Option[String],
  countyCode: Option[String]
) {
  taxPayerID.named("taxPayerID")
    .nonEmpty.trimmed.matches(TaxNumber.taxPayerIDRegex.regex)
  vatCode.named("vatCode")
    .nonEmpty.trimmed.matches(TaxNumber.vatCodeRegex.regex)
  countyCode.named("countyCode")
    .nonEmpty.trimmed.matches(TaxNumber.countyCodeRegex.regex)
}

object TaxNumber {
  final val taxPayerIDRegex: Regex = """[0-9]{8}""".r
  final val vatCodeRegex: Regex = """[1-5]""".r
  final val countyCodeRegex: Regex = """[0-9]{2}""".r
}
