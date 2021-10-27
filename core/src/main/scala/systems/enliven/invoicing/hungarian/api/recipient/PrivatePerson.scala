package systems.enliven.invoicing.hungarian.api.recipient

import systems.enliven.invoicing.hungarian.api.data.Validation
import systems.enliven.invoicing.hungarian.core.requirement.StringRequirement._
import systems.enliven.invoicing.hungarian.generated.{CustomerInfoType, PRIVATE_PERSON}

/**
  * Non-VAT taxable natural person (domestic or foreign)
  */
case class PrivatePerson(bankAccountNumber: Option[String]) extends Recipient {

  bankAccountNumber.named("bankAccountNumber").nonEmpty.trimmed.matchesAnyOf(
    Validation.bankAccountNumberRegex1.regex,
    Validation.bankAccountNumberRegex2.regex,
    Validation.bankAccountNumberRegex3.regex
  )

  /**
    * If the customer is a private individual (customerVatStatus = PRIVATE_PERSON),
    * the following nodes in the customer data cannot be populated:
    * - customerVatData
    * - customerName
    * - customerAddress
    */
  override def toCustomerInfoType: CustomerInfoType =
    CustomerInfoType(
      customerVatStatus = PRIVATE_PERSON,
      customerVatData = None,
      customerName = None,
      customerAddress = None,
      customerBankAccountNumber = bankAccountNumber
    )

}
