package systems.enliven.invoicing.hungarian

import systems.enliven.invoicing.hungarian.api.data.NavEntity

trait invoicingSuite extends baseSuite {
  protected val invoicing: Invoicing = new Invoicing()

  protected val entity: NavEntity = NavEntity.create()

  override protected def beforeAll(): Unit = {
    invoicing.awaitInit()
  }

  override protected def afterAll(): Unit = {
    invoicing.awaitShutdown()
  }

}
