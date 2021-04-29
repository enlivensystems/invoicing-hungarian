package systems.enliven.invoicing.hungarian

import org.apache.commons.lang3.RandomStringUtils
import systems.enliven.invoicing.hungarian.api.Api.Protocol.Request.Invoices
import systems.enliven.invoicing.hungarian.api.data.{Address, Issuer, Item}
import systems.enliven.invoicing.hungarian.api.recipient.Recipient

import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import javax.xml.bind.DatatypeConverter
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class suiteRequests extends invoicingSuite {

  val smartInvoices: Seq[Invoices.Invoice] = {
    TestDataGenerator.testRecipients.map {
      recipient: Recipient =>
        Invoices.Smart(
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
          recipient = recipient,
          operation = Invoices.Operation.create,
          items = Seq(
            Item(
              name = TestDataGenerator.faker.commerce().productName(),
              quantity = scala.util.Random.nextInt(10) + 1,
              price = BigDecimal(scala.util.Random.nextInt(1000) + 1),
              tax = BigDecimal("0.27"),
              intermediated = false
            )
          )
        )
    }
  }

  val invoices: Invoices = Invoices(
    Invoices.Raw(Invoices.Operation.create, DatatypeConverter.parseBase64Binary("something")) ::
      Invoices.Raw(Invoices.Operation.modify, DatatypeConverter.parseBase64Binary("something")) ::
      Invoices.Raw(Invoices.Operation.storno, DatatypeConverter.parseBase64Binary("dark side")) ::
      Nil
  )

  describe("The request API") {
    def testManageInvoice(invoices: Invoices) =
      eventually {
        val response = invoicing.invoices(invoices, entity, 10.seconds)(10.seconds)

        response match {
          case Success(response) =>
            response.result.errorCode shouldEqual None
            response.result.message shouldEqual None
            response.transactionId
          case Failure(exception) =>
            logException(exception)
        }
      }

    it("should be able to make a call to manage-invoice,") {
      testManageInvoice(invoices)
      testManageInvoice(Invoices(smartInvoices))
    }
    it(
      "should not be able to make a call to query-transaction-state with invalid transaction ID,"
    ) {
      val response =
        invoicing.status(RandomStringUtils.randomAlphanumeric(32), entity, 10.seconds)(10.seconds)

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
        invoicing.status(RandomStringUtils.randomAlphanumeric(30), entity, 10.seconds)(10.seconds)

      response match {
        case Success(response) =>
          response.processingResults.isEmpty shouldEqual true
          response.result.errorCode shouldEqual None
          response.result.message shouldEqual None
        case Failure(exception) =>
          logException(exception)
      }
    }

    it(
      "should be able to make a call to query-transaction-state with with " +
        "valid and existent transaction ID without validation errors,"
    ) {
      val transactionID = testManageInvoice(Invoices(smartInvoices))

      var i = 10
      eventually {
        if (i > 0) {
          i = i - 1
        }

        val response =
          invoicing.status(transactionID, entity, returnOriginalRequest = true, 10.seconds)(
            10.seconds
          )

        response match {
          case Success(response) =>
            log.trace(
              "Original request was [{}].",
              response.processingResults.get.processingResult.head.originalRequest.get.toString
            )

            i shouldEqual 0
            response.processingResults.isEmpty shouldEqual false
            response.processingResults.get.processingResult.forall(
              _.invoiceStatus.toString != "ABORTED"
            ) shouldEqual true
            response.processingResults.get.processingResult.head
              .technicalValidationMessages.size shouldEqual 0
            response.result.errorCode shouldEqual None
            response.result.message shouldEqual None
          case Failure(exception) =>
            logException(exception)
        }
      }
    }

    it("should be able to handle many manage-invoice calls,") {
      val testCount: Int = 100
      val completed: AtomicInteger = new AtomicInteger(0)
      (1 to testCount).par
        .map(
          _ => invoicing.invoices(invoices, entity)(40.seconds)
        )
        .foreach(_.onComplete {
          case Success(_) =>
            completed.incrementAndGet()
          case Failure(exception) =>
            logException(exception)
        }(invoicing.executionContext))

      eventually {
        completed.get() shouldEqual testCount
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
