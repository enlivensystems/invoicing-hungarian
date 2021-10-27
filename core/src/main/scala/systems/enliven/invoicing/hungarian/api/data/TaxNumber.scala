package systems.enliven.invoicing.hungarian.api.data

import systems.enliven.invoicing.hungarian.core.requirement.StringRequirement._

case class TaxNumber(
  taxpayerID: String,
  taxCode: Option[String],
  countyCode: Option[String]
) {

  taxpayerID.named("taxPayerID").nonEmpty.trimmed
    .matches(Validation.hungarianTaxpayerIDRegex.regex)

  taxCode.named("taxCode").nonEmpty.trimmed
    .matches(Validation.hungarianTaxCodeRegex.regex)

  countyCode.named("countyCode").nonEmpty.trimmed
    .matches(Validation.hungarianCountyCodeRegex.regex)

}
