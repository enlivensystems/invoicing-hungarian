package systems.enliven.invoicing.hungarian

import org.apache.commons.lang3.RandomStringUtils
import org.scalatest.funspec.AnyFunSpec
import systems.enliven.invoicing.hungarian.api.Api.Protocol.Request.Invoices
import systems.enliven.invoicing.hungarian.api.data._
import systems.enliven.invoicing.hungarian.api.recipient.Recipient

import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import javax.xml.bind.DatatypeConverter
import scala.collection.parallel.CollectionConverters.RangeIsParallelizable
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class suiteRequests extends AnyFunSpec with invoicingSuite {

  def createSmartInvoice(recipient: Recipient, vat: VAT): Invoices.Smart =
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
        bankAccountNumber = "00000000-00000000-00000000",
        smallBusiness = true,
        cashSettlement = false
      ),
      recipient = recipient,
      operation = Invoices.Operation.create,
      items = {

        val grossPrice = BigDecimal(scala.util.Random.nextInt(10000) + 1)
        val vatPrice = (grossPrice / BigDecimal(100 + vat.rate) * BigDecimal(vat.rate))
          .setScale(0, BigDecimal.RoundingMode.HALF_UP)
        Item(
          name = TestDataGenerator.faker.commerce().productName(),
          netUnitPrice = grossPrice - vatPrice,
          vatUnitPrice = vatPrice,
          grossUnitPrice = grossPrice,
          quantity = 1,
          vat = vat,
          intermediated = false
        ) :: Nil
      }
    )

  def logException(exception: Throwable): Nothing = {
    log.error(
      "Failed with [{}] with message [{}]!",
      exception.getClass.getName,
      exception.getMessage
    )
    fail
  }

  def testManageInvoice(invoices: Invoices): String =
    eventually {
      val response = invoicing.invoices(invoices, entity, 10.seconds)(10.seconds)

      response match {
        case Success(response) =>
          response.result.errorCode shouldEqual None
          response.result.message shouldEqual None
          response.transactionId
        case Failure(exception) => logException(exception)
      }
    }

  var transactionIDs: Seq[String] = Seq.empty

  val smartInvoices1: Seq[Invoices.Invoice] =
    TestDataGenerator.testRecipients.map(createSmartInvoice(_, VAT.Hungarian.Standard()))

  val smartInvoices2: Seq[Invoices.Invoice] =
    TestDataGenerator.testRecipients.map(createSmartInvoice(_, VAT.Hungarian.AAM()))

  val smartInvoices3: Seq[Invoices.Invoice] =
    TestDataGenerator.testRecipients.map(createSmartInvoice(_, VAT.Hungarian.EUFAD37()))

  val smartInvoices4: Seq[Invoices.Invoice] =
    TestDataGenerator.testRecipients.map(createSmartInvoice(_, VAT.Hungarian.HO()))

  val invoices: Invoices = Invoices(
    Invoices.Raw(Invoices.Operation.create, DatatypeConverter.parseBase64Binary("something")) ::
      Invoices.Raw(Invoices.Operation.modify, DatatypeConverter.parseBase64Binary("something")) ::
      Invoices.Raw(Invoices.Operation.storno, DatatypeConverter.parseBase64Binary("dark side")) ::
      Nil
  )

  describe("The request API") {

    it("should be able to make a call to manage-invoice,") {
      transactionIDs = transactionIDs :+ testManageInvoice(Invoices(smartInvoices1))
      transactionIDs = transactionIDs :+ testManageInvoice(Invoices(smartInvoices2))
      transactionIDs = transactionIDs :+ testManageInvoice(Invoices(smartInvoices3))
      transactionIDs = transactionIDs :+ testManageInvoice(Invoices(smartInvoices4))
      transactionIDs = transactionIDs :+ testManageInvoice(invoices)
    }

    ignore(
      "should not be able to make a call to query-transaction-state with invalid transaction ID,"
    ) {
      val response =
        invoicing.status(RandomStringUtils.randomAlphanumeric(100), entity, 10.seconds)(10.seconds)

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
      transactionIDs.init.foreach {
        transactionID =>
          eventually {
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
    }

    it("should be able to handle many manage-invoice calls,") {
      val testCount: Int = 100
      val completed: AtomicInteger = new AtomicInteger(0)
      (1 to testCount)
        .par
        .map(_ => invoicing.invoices(invoices, entity)(40.seconds))
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

    it("should be able to get invoice data,") {
      smartInvoices1.foreach {
        smartInvoice =>
          eventually {
            val response = invoicing
              .queryInvoiceData(
                smartInvoice.asInstanceOf[Invoices.Smart].number,
                entity,
                20.seconds
              )(
                20.seconds
              )
              .get

            val transactionID = response.invoiceDataResult.get.auditData.transactionId.get
            transactionIDs.contains(transactionID) shouldEqual true
          }
      }
    }
  }

}
