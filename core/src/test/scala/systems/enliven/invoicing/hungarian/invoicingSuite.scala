package systems.enliven.invoicing.hungarian

import systems.enliven.invoicing.hungarian.api.data.NavEntity
import systems.enliven.invoicing.hungarian.core.Configuration

trait invoicingSuite extends baseSuite {
  protected val invoicing: Invoicing = new Invoicing()

  protected val entity: NavEntity = createEntity()

  def getTaxNumber(accountKey: String)(implicit configuration: Configuration): String =
    configuration.get[String](s"invoicing-hungarian.accounts.$accountKey.issuer.tax-number")

  def createEntity(
    signingKeyOverride: Option[String] = None,
    exchangeKeyOverride: Option[String] = None,
    loginOverride: Option[String] = None,
    passwordOverride: Option[String] = None,
    taxNumberOverride: Option[String] = None
  )(
    implicit configuration: Configuration
  ): NavEntity =
    NavEntity(
      credentials = NavEntity.Credentials(
        signingKey = signingKeyOverride.getOrElse(configuration
          .get[String]("invoicing-hungarian.accounts.default-account.signing-key")),
        exchangeKey = exchangeKeyOverride.getOrElse(configuration
          .get[String]("invoicing-hungarian.accounts.default-account.exchange-key")),
        login = loginOverride.getOrElse(configuration
          .get[String]("invoicing-hungarian.accounts.default-account.login")),
        password = passwordOverride.getOrElse(configuration
          .get[String]("invoicing-hungarian.accounts.default-account.password"))
      ),
      taxNumber = taxNumberOverride.getOrElse(configuration
        .get[String]("invoicing-hungarian.accounts.default-account.issuer.tax-number"))
    )

  override protected def beforeAll(): Unit = {
    invoicing.awaitInit()
  }

  override protected def afterAll(): Unit = {
    invoicing.awaitShutdown()
  }

}
