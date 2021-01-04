package systems.enliven.invoicing.hungarian.api.recipient

import scalaxb.DataRecord
import scalaxb.DataRecord.__StringXMLFormat
import systems.enliven.invoicing.hungarian.api.data.Address
import systems.enliven.invoicing.hungarian.generated.{
  AddressType,
  CustomerInfoType,
  CustomerVatDataType,
  DetailedAddressType,
  OTHERValue2
}

/**
  *  Taxable person registered in another EC Member State
  */
case class EUTaxablePerson(
  communityVatNumber: String,
  name: String,
  address: Address,
  bankAccountNumber: Option[String])
 extends Recipient {
  require(communityVatNumber.matches("""[A-Z]{2}[0-9A-Z]{2,13}"""))
  require(name.nonEmpty)
  require(bankAccountNumber.forall(_.matches(
    """[0-9]{8}[-][0-9]{8}[-][0-9]{8}|[0-9]{8}[-][0-9]{8}|[A-Z]{2}[0-9]{2}[0-9A-Za-z]{11,30}"""
  )))

  /**
    * In this case, the customer's name and address (customerName, customerAddress) must be specified.
    *
    * Case A: Customer is a taxable person registered in another EC Member State,
    * and the transaction is an EC exempt supply
    *
    * Then: customerVatStatus = OTHER and communityVatNumber is mandatory
    *
    * Case B: Customer is a taxable person registered in another EC Member State,
    * and the transaction has a VAT rate of 27%, 18% or 5%.
    *
    * Then: customerVatStatus = OTHER and communityVatNumber is not mandatory, but may be provided.
    *
    * @note To simplify this communityVatNumber is mandatory.
    */
  override def toCustomerInfoType: CustomerInfoType =
    CustomerInfoType(
      customerVatStatus = OTHERValue2,
      customerVatData = Some(CustomerVatDataType(
        DataRecord[String](None, Some("communityVatNumber"), communityVatNumber)
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
