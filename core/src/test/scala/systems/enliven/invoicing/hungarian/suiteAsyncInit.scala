package systems.enliven.invoicing.hungarian

import org.scalatest.BeforeAndAfterAll

class suiteAsyncInit
  extends baseSuite
    with BeforeAndAfterAll {

  private val invoicing: Invoicing = new Invoicing()

  override protected def afterAll(): Unit = {
    invoicing.awaitShutdown()
  }

  describe("The asynchronously initialization of the API") {
    it("should work") {
      invoicing.init()
      eventually(invoicing.isReady should be (true))
    }
  }
}