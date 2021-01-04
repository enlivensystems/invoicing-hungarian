package systems.enliven.invoicing.hungarian.api.data

case class TaxNumber(
  taxPayerID: String,
  vatCode: Option[String],
  countyCode: Option[String]
) {
  require(taxPayerID.matches("""[0-9]{8}"""))
  require(vatCode.forall(_.matches("""[1-5]{1}""")))
  require(countyCode.forall(_.matches("""[0-9]{2}""")))
}
