package systems.enliven.invoicing.hungarian.api.data

import scalaxb.DataRecord
import systems.enliven.invoicing.hungarian.generated.{VatRateType, _}

sealed abstract class VAT(val rate: Int, val countryCode: String) {
  def toCode: String = countryCode + "-" + toString
}

object VAT {

  object Hungarian {
    final case object Standard extends VAT(27, "HU")

    /**
      * Alanyi adómentesség
      */
    final case object AAM extends VAT(0, "HU")

    /**
      * Áfa tárgyi hatályán kívül
      */
    final case object ATK extends VAT(0, "HU")

  }

  final val vats: Seq[VAT] = Seq(Hungarian.Standard, Hungarian.AAM, Hungarian.ATK)

  def test: VAT = vats(scala.util.Random.nextInt(vats.size))

  def fromCode(code: String): VAT =
    vats.find(_.toCode == code)
      .getOrElse(throw new NoSuchElementException(s"No value found for [$code]"))

  implicit class APIConvert(vat: VAT) {

    def toVatRate: VatRateType =
      VatRateType(
        vat match {
          case Hungarian.Standard =>
            DataRecord[BigDecimal](
              namespace = None,
              key = Some("vatPercentage"),
              value = BigDecimal(Hungarian.Standard.rate) / BigDecimal(100)
            )
          case Hungarian.AAM =>
            DataRecord[DetailedReasonType](
              namespace = None,
              key = Some("vatExemption"),
              value = DetailedReasonType("AAM", "alanyi adómentes")
            )
          case Hungarian.ATK =>
            DataRecord[DetailedReasonType](
              namespace = None,
              key = Some("vatOutOfScope"),
              value = DetailedReasonType(
                "ATK",
                "áfa tárgyi hatályán kívül"
              )
            )
        }
      )

  }

}
