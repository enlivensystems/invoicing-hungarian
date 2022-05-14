package systems.enliven.invoicing.hungarian

import org.joda.time.DateTime
import org.scalatest.funspec.AnyFunSpec
import systems.enliven.invoicing.hungarian.Invoicing.Converters.invoiceDataResultTypeParser
import systems.enliven.invoicing.hungarian.generated.OUTBOUND

import scala.concurrent.duration.DurationInt

class suiteInvoiceDigest extends AnyFunSpec with invoicingSuite {

  describe("The request API") {
    it("should be able to make a recursive call to invoice-digest and invoice-data") {
      invoicing.digestWithInvoiceData(
        entity,
        OUTBOUND,
        new DateTime.minusMinutes(5).toDate,
        new DateTime.toDate,
        Some(4),
        600.seconds
      )(600.seconds).take(400).map(_.get).foreach {
        response =>
          response.invoiceDataResult.foreach {
            data => println(data.toInvoiceData)
          }
      }
    }
    it("should be able to make a call to invoice-digest") {
      invoicing.digest(
        entity,
        OUTBOUND,
        new DateTime.minusDays(1).toDate,
        new DateTime.toDate,
        None,
        120.seconds
      )(120.seconds).get.foreach {
        response =>
          response.invoiceDigestResult.invoiceDigest.foreach {
            digest => println(digest.toString)
          }
      }
    }
  }
}
