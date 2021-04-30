package systems.enliven.invoicing.hungarian.core

class Exception(message: String) extends scala.Exception(message)

object Exception {
  case class InvalidRequestSignature(message: String) extends scala.Exception(message)
  case class InvalidExchangeKey(message: String) extends scala.Exception(message)
  case class InvalidSecurityUser(message: String) extends scala.Exception(message)
  case class NotRegisteredCustomer(message: String) extends scala.Exception(message)

}
