package systems.enliven.invoicing.hungarian

import java.util.GregorianCalendar

import javax.xml.bind.DatatypeConverter
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

  val invoices: Invoices = Invoices(
    Invoices.Raw(Invoices.Operation.create, DatatypeConverter.parseBase64Binary("something")) ::
      Invoices.Raw(
        Invoices.Operation.modify,
        DatatypeConverter.parseBase64Binary("something")
      ) ::
      Invoices.Smart(
        number = "12345",
        reference = None,
        issued = new GregorianCalendar(),
        delivered = new GregorianCalendar(),
        paid = new GregorianCalendar(),
        currencyCode = "HUF",
        exchangeRate = 0.0,
        periodical = false,
        issuer = Issuer(
          taxNumber = "",
          taxCode = "",
          taxCountry = "HU",
          communityTaxNumber = "",
          name = "",
          address = Address(
            countryCode = "",
            region = None,
            postalCode = "",
            city = "",
            streetName = "",
            publicPlaceCategory = ""
          ),
          bankAccountNumber = ""
        ),
        recipient = Recipient(
          taxNumber = None,
          taxCountry = None,
          communityTaxNumber = None,
          thirdStateTaxNumber = None,
          name = "",
          address = Address(
            countryCode = "",
            region = None,
            postalCode = "",
            city = "",
            streetName = "",
            publicPlaceCategory = ""
          ),
          bankAccountNumber = ""
        ),
        operation = Invoices.Operation.create,
        items = Seq(
          Item(
            name = "",
            quantity = 1,
            price = 100,
            intermediated = false
          )
        )
      ) ::
      Invoices.Raw(
        Invoices.Operation.storno,
        DatatypeConverter.parseBase64Binary("dark side")
      ) :: Nil
  )

  describe("The request API") {
    it("should be able to make a call to manage-invoice,") {
      Thread.sleep(2000)
      eventually {
        val response = invoicing.invoices(invoices, 10.seconds)(10.seconds)

        response match {
          case Success(response) =>
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
    }

    it("should be able to handle many manage-invoice calls,") {
      Thread.sleep(2000)
      val testCount: Int = 100
      var completed: Int = 0
      (1 to testCount)
        .map(_ => invoicing.invoices(invoices)(30.seconds))
        .foreach(_.onComplete {
          case Success(_) =>
            completed = completed + 1
          case Failure(exception) =>
            log.error(
              "Failed with [{}] with message [{}]",
              exception.getClass.getName,
              exception.getMessage
            )
        }(invoicing.executionContext))

      eventually {
        completed should be(testCount)
      }
    }
  }
}
