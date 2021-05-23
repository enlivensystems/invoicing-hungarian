package systems.enliven.invoicing.hungarian

import org.scalatest.flatspec.AnyFlatSpec
import systems.enliven.invoicing.hungarian.api.data.Entity

import scala.concurrent.duration.DurationInt
import scala.util.Try

class suiteEntityValidation extends AnyFlatSpec with invoicingSuite {

  protected val fakeSigningKey: String = "ce-8f5e-215119fa7dd621DLMRHRLH2S"

  protected val fakeExchangeKeys: Seq[String] =
    Seq("p56k867UI6HJ8965", "fake", "fdgs5", "veryVeryVeryVeryVeryVeryLongBoi")

  protected val fakeLogin: String = "p7ju87uztghh87o"
  protected val fakePassword: String = "8z7g7UZHJKH54784"
  protected val fakeTaxNumber: String = "25897878"

  def validate(entity: Entity): Try[Unit] = invoicing.validate(entity, 10.seconds)(10.seconds)

  "The API" should "be able to validate correct entities" in {
    eventually(validate(entity).get)
  }

  it should "be able to handle invalid signing-key" in {
    eventually(
      assertThrows[core.Exception.InvalidRequestSignature](
        validate(createEntity(signingKeyOverride = Some(fakeSigningKey))).get
      )
    )
  }

  it should "be able to handle invalid exchange-key" in {
    fakeExchangeKeys.foreach {
      fakeExchangeKey =>
        eventually(
          assertThrows[core.Exception.InvalidExchangeKey] {
            validate(createEntity(exchangeKeyOverride = Some(fakeExchangeKey))).get
          }
        )
    }
  }

  it should "be able to handle invalid login" in {
    eventually(
      assertThrows[core.Exception.InvalidSecurityUser](
        validate(createEntity(loginOverride = Some(fakeLogin))).get
      )
    )
  }

  it should "be able to handle invalid password" in {
    eventually(
      assertThrows[core.Exception.InvalidSecurityUser](
        validate(createEntity(passwordOverride = Some(fakePassword))).get
      )
    )
  }

  it should "be able to handle invalid tax-number" in {
    eventually(
      assertThrows[core.Exception.NotRegisteredCustomer](
        validate(createEntity(taxNumberOverride = Some(fakeTaxNumber))).get
      )
    )
  }

  it should "be able to handle valid tax-number for wrong technical user" in {

    eventually(
      assertThrows[core.Exception.InvalidUserRelation](
        validate(createEntity(taxNumberOverride = Some(getTaxNumber("secondary-account")))).get
      )
    )
  }

}
