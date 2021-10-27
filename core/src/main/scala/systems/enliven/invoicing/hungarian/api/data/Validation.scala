package systems.enliven.invoicing.hungarian.api.data

import scala.util.matching.Regex

object Validation {
  final val hungarianTaxNumberParser: Regex = """([0-9]{8})-([1-5])-([0-9]{2})""".r
  final val communityTaxNumberParser: Regex = """([A-Z]{2})([0-9A-Z]{2,13})""".r

  final val hungarianTaxpayerIDRegex: Regex = """[0-9]{8}""".r
  final val hungarianTaxCodeRegex: Regex = """[1-5]""".r
  final val hungarianCountyCodeRegex: Regex = """[0-9]{2}""".r

  final val communityCountryCodeRegex: Regex = """[A-Z]{2}""".r
  final val communityTaxpayerIDRegex: Regex = """[0-9A-Z]{2,13}""".r

  final val NAVLoginNameRegex: Regex = """[a-zA-Z0-9]{6,15}""".r

  final val bankAccountNumberRegex1: Regex = """[0-9]{8}-[0-9]{8}-[0-9]{8}""".r
  final val bankAccountNumberRegex2: Regex = """[0-9]{8}-[0-9]{8}""".r
  final val bankAccountNumberRegex3: Regex = """[A-Z]{2}[0-9]{2}[0-9A-Za-z]{11,30}""".r

  final val countryCodeRegex: Regex = """[A-Z]{2}""".r // ISO-3166 alpha 2
  final val regionCodeRegex: Regex = """[A-Z0-9]{1,3}""".r
  final val postalCodeRegex: Regex = """[A-Z0-9][A-Z0-9\s\-]{1,8}[A-Z0-9]""".r

}
