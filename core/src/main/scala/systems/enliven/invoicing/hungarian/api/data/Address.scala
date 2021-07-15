package systems.enliven.invoicing.hungarian.api.data

import systems.enliven.invoicing.hungarian.generated.DetailedAddressType
import systems.enliven.invoicing.hungarian.core.requirement.StringRequirement._

import scala.util.matching.Regex

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
  countryCode.named("countryCode")
    .nonEmpty.trimmed.matches(Address.countryCodeRegex.regex) // ISO-3166 alpha 2
  region.named("region")
    .nonEmpty.trimmed.matches(Address.regionCodeRegex.regex)
  postalCode.named("postalCode")
    .nonEmpty.trimmed.matches(Address.postalCodeRegex.regex)
  city.named("city").nonEmpty.trimmed
  streetName.named("streetName").nonEmpty.trimmed
  publicPlaceCategory.named("publicPlaceCategory").nonEmpty.trimmed
  number.named("number").nonEmpty.trimmed
  building.named("building").nonEmpty.trimmed
  staircase.named("staircase").nonEmpty.trimmed
  floor.named("floor").nonEmpty.trimmed
  door.named("door").nonEmpty.trimmed
  lotNumber.named("lotNumber").nonEmpty.trimmed
}

object Address {
  final val countryCodeRegex: Regex = """[A-Z]{2}""".r
  final val regionCodeRegex: Regex = """[A-Z0-9]{1,3}""".r
  final val postalCodeRegex: Regex = """[A-Z0-9][A-Z0-9\s\-]{1,8}[A-Z0-9]""".r

  def create(detailedAddress: DetailedAddressType): Address =
    Address(
      countryCode = detailedAddress.countryCode,
      region = detailedAddress.region,
      postalCode = detailedAddress.postalCode,
      city = detailedAddress.city,
      streetName = detailedAddress.streetName,
      publicPlaceCategory = detailedAddress.publicPlaceCategory,
      number = detailedAddress.number,
      building = detailedAddress.building,
      staircase = detailedAddress.staircase,
      floor = detailedAddress.floor,
      door = detailedAddress.door,
      lotNumber = detailedAddress.lotNumber
    )

}
