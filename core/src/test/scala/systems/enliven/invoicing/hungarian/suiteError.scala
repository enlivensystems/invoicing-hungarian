package systems.enliven.invoicing.hungarian

import javax.xml.bind.DatatypeConverter
import systems.enliven.invoicing.hungarian.api.Api.Protocol.Request.Invoices

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class suiteError extends invoicingSuite {

  val invoice: Invoices.Invoice =
    Invoices.Raw(Invoices.Operation.storno, DatatypeConverter.parseBase64Binary("something"))

  val validInvoices = (1 to 100).map(_ => invoice)

  describe("The request API") {

    it("should not be able to manage 0 invoices,") {
      val response = invoicing.invoices(Invoices(Nil), 10.seconds)(10.seconds)

      response match {
        case Success(_) => fail
        case Failure(exception) =>
          log.error(
            "Failed with [{}] with message [{}]",
            exception.getClass.getName,
            exception.getMessage
          )
          exception.getMessage.contains("INVALID_REQUEST") should be(true)
          exception.getMessage.contains("SCHEMA_VIOLATION") should be(true)
      }
    }

    it("should be able to manage 100 invoices,") {
      val response = invoicing.invoices(Invoices(validInvoices), 10.seconds)(10.seconds)

      response match {
        case Success(response) =>
          log.info(response.toString)
          response.result.errorCode should be(None)
          response.result.message should be(None)
        case Failure(exception) =>
          log.error(
            "Failed with [{}] with message [{}]",
            exception.getClass.getName,
            exception.getMessage
          )
          fail
      }
    }

    it("should not be able to manage 101 invoices,") {
      val response = invoicing.invoices(Invoices(validInvoices :+ invoice), 10.seconds)(10.seconds)

      response match {
        case Success(_) => fail
        case Failure(exception) =>
          log.error(
            "Failed with [{}] with message [{}]",
            exception.getClass.getName,
            exception.getMessage
          )
          exception.getMessage.contains("INVALID_REQUEST") should be(true)
          exception.getMessage.contains("SCHEMA_VIOLATION") should be(true)
      }
    }
  }

}
