package systems.enliven.invoicing.hungarian

import org.scalatest.funspec.AnyFunSpec
import scalaxb.Base64Binary
import systems.enliven.invoicing.hungarian.api.data.Entity
import systems.enliven.invoicing.hungarian.api.{Api, Hash}
import systems.enliven.invoicing.hungarian.generated.{
  CREATE, InvoiceOperationListType, InvoiceOperationType, MODIFY
}

import java.time.Instant

class suiteSignature extends AnyFunSpec with invoicingSuite {

  override protected val entity: Entity =
    createEntity(signingKeyOverride = Some("ce-8f5e-215119fa7dd621DLMRHRLH2S"))

  protected val api: Api =
    new Api()(configuration, invoicing.typedSystem.classicSystem, invoicing.executionContext)

  describe("The requests API") {
    it("should be able to correctly generate a signature.") {
      api.builder.buildRequestSignature(
        entity,
        "TSTKFT1222564",
        Instant.parse("2017-12-30T18:25:45.000Z"),
        InvoiceOperationListType(
          compressedContent = false,
          Seq(
            InvoiceOperationType(
              1,
              CREATE,
              Base64Binary("QWJjZDEyMzQ=")
            ),
            InvoiceOperationType(
              2,
              MODIFY,
              Base64Binary("RGNiYTQzMjE=")
            )
          )
        )
      ) shouldEqual Hash.hashSHA3512(
        "TSTKFT122256420171230182545ce-8f5e-215119fa7dd621DLMRHRLH2S4317798460962869BC" +
          "67F07C48EA7E4A3AFA301513CEB87B8EB94ECF92BC220A89C480F87F0860E85E29A3B6C0463D4F29712C5A" +
          "D48104A6486CE839DC2F24CBA881218238933F6FFB9E167445CB4DAA9749BCF484FDE48AB7649FD25E8B63" +
          "4A4736A65A7C4A8E2831119F739837E006566F97370415AAD55E268605206F2A6C"
      )
    }
  }
}
