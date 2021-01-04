package systems.enliven.invoicing.hungarian.api.recipient

import scalaxb.DataRecord
import systems.enliven.invoicing.hungarian.api.data.{Address, TaxNumber}
import systems.enliven.invoicing.hungarian.generated.{
  AddressType,
  CustomerInfoType,
  CustomerTaxNumberType,
  CustomerVatDataType,
  DOMESTIC,
  DetailedAddressType
}

/**
  *  Domestic VAT taxable person
  */
case class DomesticTaxablePerson(
  taxNumber: TaxNumber,
  name: String,
  address: Address,
  bankAccountNumber: Option[String])
 extends Recipient {
  require(name.nonEmpty)
  require(bankAccountNumber.forall(_.matches(
    """[0-9]{8}[-][0-9]{8}[-][0-9]{8}|[0-9]{8}[-][0-9]{8}|[A-Z]{2}[0-9]{2}[0-9A-Za-z]{11,30}"""
  )))

  /**
    * If the customer status is domestic taxable person (customerVatStatus = DOMESTIC),
    * the VAT registered issuer of the invoice has to indicate the tax number of the domestic
    * taxable person, save for transactions subject to direct taxation. If the customer is a domestic taxable
    * person (customerVatStatus = DOMESTIC) and no domestic tax number is indicated (customerTaxNumber),
    * the data report will not be accepted by the tax authority system (save for VAT registered sellers).
    */
  override def toCustomerInfoType: CustomerInfoType =
    CustomerInfoType(
      customerVatStatus = DOMESTIC,
      customerVatData = Some(CustomerVatDataType(
        DataRecord[CustomerTaxNumberType](
          None,
          Some("customerTaxNumber"),
          CustomerTaxNumberType(
            taxpayerId = taxNumber.taxPayerID,
            vatCode = taxNumber.vatCode,
            countyCode = taxNumber.countyCode,
            groupMemberTaxNumber = None
          )
        )
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
