package systems.enliven.invoicing.hungarian.api.data

import systems.enliven.invoicing.hungarian.core.requirement.StringRequirement._
import systems.enliven.invoicing.hungarian.generated.DetailedAddressType

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

  countryCode.named("countryCode").nonEmpty.trimmed
    .matches(Validation.countryCodeRegex.regex)

  region.named("region").nonEmpty.trimmed
    .matches(Validation.regionCodeRegex.regex)

  postalCode.named("postalCode").nonEmpty.trimmed
    .matches(Validation.postalCodeRegex.regex)

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
