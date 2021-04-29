package systems.enliven.invoicing.hungarian

import org.scalatest.BeforeAndAfterAll
import systems.enliven.invoicing.hungarian.api.data.NavEntity

class invoicingSuite extends baseSuite with BeforeAndAfterAll {
  protected val invoicing: Invoicing = new Invoicing()

  protected val entity: NavEntity = NavEntity.create()

  override protected def beforeAll(): Unit = {
    invoicing.awaitInit()
  }

  override protected def afterAll(): Unit = {
    invoicing.awaitShutdown()
  }

}
