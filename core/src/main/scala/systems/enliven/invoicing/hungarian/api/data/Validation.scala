package systems.enliven.invoicing.hungarian.api.data

import scala.util.matching.Regex

object Validation {
  final val hungarianTaxpayerIDRegex: Regex = """[0-9]{8}""".r
  final val hungarianTaxCodeRegex: Regex = """[1-5]""".r

  /**
    * @note Accepted values: 02-20|22-44|51 (From NAV Docs [INCORRECT_COUNTY_CODE_CUSTOMER])
    */
  final val hungarianCountyCodeRegex: Regex = """0[2-9]|1[0-9]|20|2[2-9]|3[0-9]|4[0-4]|51""".r

  final val communityCountryCodeRegex: Regex = """[A-Z]{2}""".r
  final val communityTaxpayerIDRegex: Regex = """[0-9A-Z]{2,13}""".r

  final val hungarianTaxNumberParser: Regex =
    new Regex(
      s"""($hungarianTaxpayerIDRegex)-($hungarianTaxCodeRegex)-($hungarianCountyCodeRegex)"""
    )

  final val communityTaxNumberParser: Regex =
    new Regex(
      s"""($communityCountryCodeRegex)($communityTaxpayerIDRegex)"""
    )

  final val NAVLoginNameRegex: Regex = """[a-zA-Z0-9]{6,15}""".r

  final val bankAccountNumberRegex1: Regex = """[0-9]{8}-[0-9]{8}-[0-9]{8}""".r
  final val bankAccountNumberRegex2: Regex = """[0-9]{8}-[0-9]{8}""".r
  final val bankAccountNumberRegex3: Regex = """[A-Z]{2}[0-9]{2}[0-9A-Za-z]{11,30}""".r

  final val countryCodeRegex: Regex = """[A-Z]{2}""".r // ISO-3166 alpha 2
  final val regionCodeRegex: Regex = """[A-Z0-9]{1,3}""".r
  final val postalCodeRegex: Regex = """[A-Z0-9][A-Z0-9\s\-]{1,8}[A-Z0-9]""".r

}
