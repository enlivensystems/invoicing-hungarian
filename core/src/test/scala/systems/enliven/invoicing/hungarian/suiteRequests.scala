package systems.enliven.invoicing.hungarian

import java.util.Date

import javax.xml.bind.DatatypeConverter
import org.apache.commons.lang3.RandomStringUtils
import systems.enliven.invoicing.hungarian.api.Api.Protocol.Request.Invoices
import systems.enliven.invoicing.hungarian.api.Api.Protocol.Request.Invoices.{
  Address,
  Issuer,
  Item,
  Recipient
}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class suiteRequests extends invoicingSuite {

  val smartInvoice: Invoices.Invoice = Invoices.Smart(
    number = RandomStringUtils.randomAlphanumeric(32),
    reference = None,
    issued = new Date(),
    delivered = new Date(),
    paid = new Date(),
    currencyCode = "HUF",
    exchangeRate = 1,
    periodical = false,
    issuer = Issuer(
      taxNumber = "25962295",
      taxCode = "2",
      taxCounty = "07",
      communityTaxNumber = "HU25962295",
      name = "W",
      address = Address(
        countryCode = "HU",
        region = None,
        postalCode = "1092",
        city = "W",
        streetName = "W",
        publicPlaceCategory = "W"
      ),
      bankAccountNumber = "00000000-00000000-00000000"
    ),
    recipient = Recipient(
      taxNumber = None,
      taxCountry = None,
      communityTaxNumber = None,
      thirdStateTaxNumber = None,
      name = "W",
      address = Address(
        countryCode = "HU",
        region = None,
        postalCode = "1092",
        city = "W",
        streetName = "W",
        publicPlaceCategory = "W"
      ),
      bankAccountNumber = Some("00000000-00000000-00000000")
    ),
    operation = Invoices.Operation.create,
    items = Seq(
      Item(
        name = "W",
        quantity = 1,
        price = 100,
        tax = 0.27,
        intermediated = false
      )
    )
  )

  val invoices: Invoices = Invoices(
    Invoices.Raw(Invoices.Operation.create, DatatypeConverter.parseBase64Binary("something")) ::
      Invoices.Raw(
        Invoices.Operation.modify,
        DatatypeConverter.parseBase64Binary("something")
      ) ::
      smartInvoice ::
      Invoices.Raw(
        Invoices.Operation.storno,
        DatatypeConverter.parseBase64Binary("dark side")
      ) :: Nil
  )

  describe("The request API") {
    def testManageInvoice(invoices: Invoices) =
      eventually {
        val response = invoicing.invoices(invoices, 10.seconds)(10.seconds)

        response match {
          case Success(response) =>
            response.result.errorCode should be(None)
            response.result.message should be(None)
            response.transactionId
          case Failure(exception) =>
            logException(exception)
        }
      }

    it("should be able to make a call to manage-invoice,") {
      testManageInvoice(invoices)
    }
    it(
      "should not be able to make a call to query-transaction-state with invalid transaction ID,"
    ) {
      val response =
        invoicing.status(RandomStringUtils.randomAlphanumeric(32), 10.seconds)(10.seconds)

      response match {
        case Success(_) =>
          fail
        case Failure(exception) =>
          log.info("Failed with invalid transaction ID and message [{}].", exception.getMessage)
      }
    }

    it(
      "should be able to make a call to query-transaction-state with with " +
        "valid but non-existent transaction ID,"
    ) {
      val response =
        invoicing.status(RandomStringUtils.randomAlphanumeric(30), 10.seconds)(10.seconds)

      response match {
        case Success(response) =>
          response.processingResults.isEmpty should be(true)
          response.result.errorCode should be(None)
          response.result.message should be(None)
        case Failure(exception) =>
          logException(exception)
      }
    }

    it(
      "should be able to make a call to query-transaction-state with with " +
        "valid and existent transaction ID without validation errors,"
    ) {
      val transactionID = testManageInvoice(Invoices(Seq(smartInvoice)))

      var i = 10
      eventually {
        if (i > 0) {
          i = i - 1
        }

        val response =
          invoicing.status(transactionID, returnOriginalRequest = true, 10.seconds)(10.seconds)

        response match {
          case Success(response) =>
            log.trace(
              "Original request was [{}].",
              response.processingResults.get.processingResult.head.originalRequest.get.toString
            )

            i should be(0)
            response.processingResults.isEmpty should be(false)
            response.processingResults.get.processingResult.forall(
              _.invoiceStatus.toString != "ABORTED"
            ) should be(true)
            response.processingResults.get.processingResult.head
              .technicalValidationMessages.size should be(
              0
            )
            response.result.errorCode should be(None)
            response.result.message should be(None)
          case Failure(exception) =>
            logException(exception)
        }
      }
    }

    it("should be able to handle many manage-invoice calls,") {
      val testCount: Int = 100
      var completed: Int = 0
      (1 to testCount).par
        .map(
          _ => invoicing.invoices(invoices)(40.seconds)
        )
        .foreach(_.onComplete {
          case Success(_) =>
            completed = completed + 1
          case Failure(exception) =>
            logException(exception)
        }(invoicing.executionContext))

      eventually {
        completed should be(testCount)
      }
    }
  }

  def logException(exception: Throwable) = {
    log.error(
      "Failed with [{}] with message [{}]!",
      exception.getClass.getName,
      exception.getMessage
    )
    fail
  }

}
