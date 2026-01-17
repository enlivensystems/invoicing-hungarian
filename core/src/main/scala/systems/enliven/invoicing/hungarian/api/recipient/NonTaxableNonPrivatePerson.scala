package systems.enliven.invoicing.hungarian.api.recipient

import scalaxb.DataRecord
import systems.enliven.invoicing.hungarian.api.data.{Address, Validation}
import systems.enliven.invoicing.hungarian.core.requirement.StringRequirement._
import systems.enliven.invoicing.hungarian.generated.{
  AddressType, CustomerInfoType, DetailedAddressType, OTHERValue2
}

/**
  * The customer is not a taxable person, but it is not a private individual either.
  */
case class NonTaxableNonPrivatePerson(
  name: String,
  address: Address,
  bankAccountNumber: Option[String])
 extends Recipient {

  name.named("name").nonEmpty.trimmed

  bankAccountNumber.named("bankAccountNumber").nonEmpty.trimmed.matchesAnyOf(
    Validation.bankAccountNumberRegex1.regex,
    Validation.bankAccountNumberRegex2.regex,
    Validation.bankAccountNumberRegex3.regex
  )

  /**
    * In this case, the customer's name and address (customerName, customerAddress) must be specified.
    * customerVatStatus = OTHER and no element of customerVatData can be provided
    * since the customer has no tax number.
    */
  override def toCustomerInfoType: CustomerInfoType =
    CustomerInfoType(
      customerVatStatus = OTHERValue2,
      customerVatData = None,
      customerName = Some(name),
      customerAddress = Some(AddressType(
        DataRecord[DetailedAddressType](
          namespace = Some("http://schemas.nav.gov.hu/OSA/3.0/base"),
          key = Some("detailedAddress"),
          value = DetailedAddressType(
            countryCode = address.countryCode,
            region = address.region,
            postalCode = address.postalCode,
            city = address.city,
            streetName = address.streetName,
            publicPlaceCategory = address.publicPlaceCategory,
            number = address.number,
            building = address.building,
            staircase = address.staircase,
            floor = address.floor,
            door = address.door,
            lotNumber = address.lotNumber
          )
        )
      )),
      customerBankAccountNumber = bankAccountNumber
    )

}
