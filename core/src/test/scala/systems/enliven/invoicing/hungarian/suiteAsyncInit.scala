package systems.enliven.invoicing.hungarian

import org.scalatest.funspec.AnyFunSpec

class suiteAsyncInit extends AnyFunSpec with baseSuite {

  private val invoicing: Invoicing = new Invoicing()

  override protected def afterAll(): Unit = {
    invoicing.awaitShutdown()
  }

  describe("The asynchronously initialization of the API") {
    it("should work") {
      invoicing.init()
      eventually(invoicing.isReady should be(true))
    }
  }
}
