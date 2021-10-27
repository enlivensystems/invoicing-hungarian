package systems.enliven.invoicing.hungarian.api.recipient

import scalaxb.DataRecord
import systems.enliven.invoicing.hungarian.api.data.{Address, TaxNumber, Validation}
import systems.enliven.invoicing.hungarian.core.requirement.StringRequirement._
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

  require(groupTaxNumber.taxCode.forall(_.equals("5")))
  require(memberTaxNumber.taxCode.forall(_.equals("4")))
  name.named("name").nonEmpty.trimmed

  bankAccountNumber.named("bankAccountNumber").nonEmpty.trimmed.matchesAnyOf(
    Validation.bankAccountNumberRegex1.regex,
    Validation.bankAccountNumberRegex2.regex,
    Validation.bankAccountNumberRegex3.regex
  )

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
            taxpayerId = groupTaxNumber.taxpayerID,
            vatCode = groupTaxNumber.taxCode,
            countyCode = groupTaxNumber.countyCode,
            groupMemberTaxNumber = Some(TaxNumberType(
              taxpayerId = memberTaxNumber.taxpayerID,
              vatCode = memberTaxNumber.taxCode,
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
