package systems.enliven.invoicing.hungarian

import org.scalatest.BeforeAndAfterAll

class invoicingSuite extends baseSuite with BeforeAndAfterAll {
  protected val invoicing: Invoicing = new Invoicing()

  override protected def beforeAll(): Unit = {
    invoicing.awaitInit()
  }

  override protected def afterAll(): Unit = {
    invoicing.awaitShutdown()
  }

}
