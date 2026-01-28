package systems.enliven.invoicing.hungarian.api.recipient

import scalaxb.DataRecord
import scalaxb.DataRecord.__StringXMLFormat
import systems.enliven.invoicing.hungarian.api.data.{Address, Validation}
import systems.enliven.invoicing.hungarian.core.requirement.StringRequirement._
import systems.enliven.invoicing.hungarian.generated.{
  AddressType,
  CustomerInfoType,
  CustomerVatDataType,
  DetailedAddressType,
  OTHERValue2
}

/**
  *  Taxable person registered in a non-EU country
  */
case class NonEUTaxablePerson(
  thirdStateTaxNumber: String,
  name: String,
  address: Address,
  bankAccountNumber: Option[String])
 extends Recipient {

  thirdStateTaxNumber.named("thirdStateTaxNumber").nonEmpty.trimmed
  name.named("name").nonEmpty.trimmed

  bankAccountNumber.named("bankAccountNumber").nonEmpty.trimmed.matchesAnyOf(
    Validation.bankAccountNumberRegex1.regex,
    Validation.bankAccountNumberRegex2.regex,
    Validation.bankAccountNumberRegex3.regex
  )

  /**
    * The customer is registered in a non-EU country, and does not participate in the transaction as taxable
    * person registered in another Member State.
    *
    * In this case, the customer's name and address (customerName, customerAddress) must be specified.
    * customerVatStatus = OTHER and thirdStateTaxId is not mandatory, but may be provided.
    */
  override def toCustomerInfoType: CustomerInfoType =
    CustomerInfoType(
      customerVatStatus = OTHERValue2,
      customerVatData = Some(CustomerVatDataType(
        DataRecord[String](None, Some("thirdStateTaxId"), thirdStateTaxNumber)
      )),
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
