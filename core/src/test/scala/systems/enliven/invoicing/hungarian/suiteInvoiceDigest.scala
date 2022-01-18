package systems.enliven.invoicing.hungarian

import org.joda.time.DateTime
import org.scalatest.funspec.AnyFunSpec
import systems.enliven.invoicing.hungarian.generated.INBOUND

import scala.concurrent.duration.DurationInt

class suiteInvoiceDigest extends AnyFunSpec with invoicingSuite {

  describe("The request API") {

    it("should be able to make a call to invoice-digest,") {
      invoicing.digest(
        entity,
        INBOUND,
        new DateTime().minusMonths(1).toDate,
        new DateTime().toDate,
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
