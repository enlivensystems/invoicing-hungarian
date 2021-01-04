package systems.enliven.invoicing.hungarian.api.recipient

import scalaxb.DataRecord
import systems.enliven.invoicing.hungarian.api.data.{Address, TaxNumber}
import systems.enliven.invoicing.hungarian.generated.{
  AddressType,
  CustomerInfoType,
  CustomerTaxNumberType,
  CustomerVatDataType,
  DOMESTIC,
  DetailedAddressType,
  TaxNumberType
}

/**
  * Domestic group VAT subject
  */
case class DomesticGroupVATSubject(
  groupTaxNumber: TaxNumber,
  memberTaxNumber: TaxNumber,
  name: String,
  address: Address,
  bankAccountNumber: Option[String]
)
 extends Recipient {
  require(groupTaxNumber.vatCode.forall(_.equals("5")))
  require(memberTaxNumber.vatCode.forall(_.equals("4")))
  require(name.nonEmpty)
  require(bankAccountNumber.forall(_.matches(
    """[0-9]{8}[-][0-9]{8}[-][0-9]{8}|[0-9]{8}[-][0-9]{8}|[A-Z]{2}[0-9]{2}[0-9A-Za-z]{11,30}"""
  )))

  /**
    * If the customer is a domestic group VAT subject, then the customer’s tax number should be
    * the group identification number, and the group member's tax number should be entered into
    * the groupMemberTaxNumber element (type: TaxNumberType),
    * provided it is included in the invoice.
    */
  override def toCustomerInfoType: CustomerInfoType =
    CustomerInfoType(
      customerVatStatus = DOMESTIC,
      customerVatData = Some(CustomerVatDataType(
        DataRecord[CustomerTaxNumberType](
          None,
          Some("customerTaxNumber"),
          CustomerTaxNumberType(
            taxpayerId = groupTaxNumber.taxPayerID,
            vatCode = groupTaxNumber.vatCode,
            countyCode = groupTaxNumber.countyCode,
            groupMemberTaxNumber = Some(TaxNumberType(
              taxpayerId = memberTaxNumber.taxPayerID,
              vatCode = memberTaxNumber.vatCode,
              countyCode = memberTaxNumber.countyCode
            ))
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
