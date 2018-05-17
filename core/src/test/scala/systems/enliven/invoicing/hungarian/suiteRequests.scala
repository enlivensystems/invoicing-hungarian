package systems.enliven.invoicing.hungarian

import javax.xml.bind.DatatypeConverter
import systems.enliven.invoicing.hungarian.api.Api.Protocol.Request.Invoices

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class suiteRequests extends invoicingSuite {

  val invoices: Invoices = Invoices(
    Invoices.Invoice(Invoices.Operation.create, DatatypeConverter.parseBase64Binary("something")) ::
      Invoices.Invoice(Invoices.Operation.modify, DatatypeConverter.parseBase64Binary("something")) ::
      Invoices.Invoice(Invoices.Operation.storno, DatatypeConverter.parseBase64Binary("dark side")) :: Nil
  )

  describe("The request API") {
    it("should be able to make a call to manage-invoice,") {
      Thread.sleep(2000)
      eventually {
        val response = invoicing.invoices(invoices, 10.seconds)(10.seconds)

        response match {
          case Success(response) =>
            response.result.errorCode should be (None)
            response.result.message should be (None)
          case Failure(exception) =>
            log.error("Failed with [{}] with message [{}]",
              exception.getClass.getName,
              exception.getMessage)
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
            log.error("Failed with [{}] with message [{}]",
              exception.getClass.getName,
              exception.getMessage)
        }(invoicing.executionContext))

      eventually {
        completed should be (testCount)
      }
    }
  }
}