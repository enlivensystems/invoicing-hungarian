package systems.enliven.invoicing.hungarian.api.recipient

import systems.enliven.invoicing.hungarian.generated.{CustomerInfoType, PRIVATE_PERSON}

/**
  * Non-VAT taxable natural person (domestic or foreign)
  */
case class PrivatePerson(bankAccountNumber: Option[String]) extends Recipient {
  require(bankAccountNumber.forall(_.matches(
    """[0-9]{8}[-][0-9]{8}[-][0-9]{8}|[0-9]{8}[-][0-9]{8}|[A-Z]{2}[0-9]{2}[0-9A-Za-z]{11,30}"""
  )))

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
