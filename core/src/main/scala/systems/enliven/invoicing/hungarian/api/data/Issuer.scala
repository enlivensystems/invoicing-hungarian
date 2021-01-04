package systems.enliven.invoicing.hungarian.api.data

case class Issuer(
  taxNumber: String,
  taxCode: String,
  taxCounty: String,
  communityTaxNumber: String,
  name: String,
  address: Address,
  bankAccountNumber: String) {
  require(taxCode.matches("""[1-5]"""))
  require(taxCounty.matches("""[0-9]{2}"""))
  require(communityTaxNumber.matches("""[A-Z]{2}[0-9A-Z]{2,13}"""))
  require(name.nonEmpty)
  require(bankAccountNumber.nonEmpty)
}
