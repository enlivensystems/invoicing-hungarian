package systems.enliven.invoicing.hungarian.api.data

case class Address(
  countryCode: String,
  region: Option[String] = None,
  postalCode: String,
  city: String,
  streetName: String,
  publicPlaceCategory: String,
  number: Option[String] = None,
  building: Option[String] = None,
  staircase: Option[String] = None,
  floor: Option[String] = None,
  door: Option[String] = None,
  lotNumber: Option[String] = None) {
  require(countryCode.matches("""[A-Z]{2}""")) // ISO-3166 alpha 2
  require(region.forall(_.matches("""[A-Z]{2}"""))) // ISO-3166 alpha 2
  require(postalCode.matches("""[A-Z0-9]{4,10}"""))
  require(city.nonEmpty)
  require(streetName.nonEmpty)
  require(publicPlaceCategory.nonEmpty)
  require(number.forall(_.nonEmpty))
  require(building.forall(_.nonEmpty))
  require(staircase.forall(_.nonEmpty))
  require(floor.forall(_.nonEmpty))
  require(door.forall(_.nonEmpty))
  require(lotNumber.forall(_.nonEmpty))
}
