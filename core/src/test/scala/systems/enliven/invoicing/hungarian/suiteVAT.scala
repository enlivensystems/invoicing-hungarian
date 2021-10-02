package systems.enliven.invoicing.hungarian

import org.scalatest.funspec.AnyFunSpec
import systems.enliven.invoicing.hungarian.api.data.VAT

class suiteVAT extends AnyFunSpec with baseSuite {

  describe("The VAT codes should be correctly generated, ") {
    VAT.Hungarian.Standard().globalCode shouldEqual "HU-Standard"
    VAT.Hungarian.AAM().globalCode shouldEqual "HU-AAM"
    VAT.Hungarian.EUFAD37().globalCode shouldEqual "HU-EUFAD37"
    VAT.Hungarian.HO().globalCode shouldEqual "HU-HO"
  }

}
