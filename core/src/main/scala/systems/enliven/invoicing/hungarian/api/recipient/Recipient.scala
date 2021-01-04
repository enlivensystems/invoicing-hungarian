package systems.enliven.invoicing.hungarian.api.recipient

import systems.enliven.invoicing.hungarian.generated.CustomerInfoType

trait Recipient {
  def toCustomerInfoType: CustomerInfoType
}
