package systems.enliven.invoicing.hungarian

import systems.enliven.invoicing.hungarian.api.Hash

class suiteHashers extends baseSuite {
  describe("The hashers") {
    it("should be able to correctly hash SHA-512") {
      (1 to 10) foreach {
        _ =>
          Hash.hashSHA512("4f3h8AKHGFE67342") should be (
            "42D4F8123392D5A284E609A159B5C5DA1754D922B250295FEF0575451B" +
            "CF0B791128DC445C6EB847B8CCBC3A881015A9E39BE4332954E9A9B66925F11DCF7535")
      }
    }
  }
}