package systems.enliven.invoicing.hungarian

import org.scalatest.flatspec.AnyFlatSpec

import scala.concurrent.duration.DurationInt

class suiteQueryTaxpayer extends AnyFlatSpec with invoicingSuite {

  def queryTaxpayer(taxNumber: String): scala.util.Try[api.data.Taxpayer] =
    invoicing.queryTaxpayer(taxNumber, entity, 10.seconds)(10.seconds)

  "The API" should "be able to query taxpayer data" in {
    eventually {
      queryTaxpayer(getTaxNumber("default-account")).get
    }

    eventually {
      queryTaxpayer(getTaxNumber("secondary-account")).get
    }
  }

}
