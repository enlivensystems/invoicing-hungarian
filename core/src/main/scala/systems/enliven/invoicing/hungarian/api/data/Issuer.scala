package systems.enliven.invoicing.hungarian.api.data

import systems.enliven.invoicing.hungarian.core.requirement.StringRequirement._

case class Issuer(
  taxNumber: String,
  taxCode: String,
  taxCounty: String,
  communityTaxNumber: String,
  name: String,
  address: Address,
  bankAccountNumber: String,
  /**
    * "Kisadózó".
    */
  smallBusiness: Boolean,
  /**
    * "Pénzforgalmi elszámolás".
    */
  cashSettlement: Boolean
) {

  taxNumber.named("taxNumber").nonEmpty.trimmed

  taxCode.named("taxCode").nonEmpty.trimmed
    .matches(Validation.hungarianTaxCodeRegex.regex)

  taxCounty.named("taxCounty").nonEmpty.trimmed
    .matches(Validation.hungarianCountyCodeRegex.regex)

  communityTaxNumber.named("communityTaxNumber").nonEmpty.trimmed
    .matches(Validation.communityTaxNumberParser.regex)

  name.named("name").nonEmpty.trimmed

  bankAccountNumber.named("bankAccountNumber").nonEmpty.trimmed.matchesAnyOf(
    Validation.bankAccountNumberRegex1.regex,
    Validation.bankAccountNumberRegex2.regex,
    Validation.bankAccountNumberRegex3.regex
  )

}
