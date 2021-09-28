package systems.enliven.invoicing.hungarian.api.data

import scalaxb.DataRecord
import systems.enliven.invoicing.hungarian.generated.{VatRateType, _}

sealed abstract class VAT(val rate: Int, val countryCode: String) extends Serializable {
  def toCode: String = countryCode + "-" + toString
}

object VAT {

  object Hungarian {
    final case class Standard() extends VAT(27, "HU")

    /**
      * Alanyi adómentesség
      */
    final case class AAM() extends VAT(0, "HU")

    /**
      * Áfa törvény 37. paragrafusa alapján másik tagállamban teljesített, fordítottan adózó ügylet
      */
    final case class EUFAD37() extends VAT(0, "HU")

    /**
      * Harmadik országban teljesített ügylet
      */
    final case class HO() extends VAT(0, "HU")

  }

  final val vats: Seq[VAT] = Seq(
    Hungarian.Standard(),
    Hungarian.AAM(),
    Hungarian.EUFAD37(),
    Hungarian.HO()
  )

  def test: VAT = vats(scala.util.Random.nextInt(vats.size))

  def fromCode(code: String): VAT =
    vats.find(_.toCode == code)
      .getOrElse(throw new NoSuchElementException(s"No value found for [$code]"))

  implicit class APIConvert(vat: VAT) {

    def toVatRate: VatRateType =
      VatRateType(
        vat match {
          case standard: Hungarian.Standard =>
            DataRecord[BigDecimal](
              namespace = None,
              key = Some("vatPercentage"),
              value = BigDecimal(standard.rate) / BigDecimal(100)
            )
          case Hungarian.AAM() =>
            DataRecord[DetailedReasonType](
              namespace = None,
              key = Some("vatExemption"),
              value = DetailedReasonType("AAM", "alanyi adómentes")
            )
          case Hungarian.EUFAD37() =>
            DataRecord[DetailedReasonType](
              namespace = None,
              key = Some("vatOutOfScope"),
              value = DetailedReasonType(
                "EUFAD37",
                "áfa törvény 37. paragrafusa alapján másik tagállamban teljesített, fordítottan adózó ügylet"
              )
            )
          case Hungarian.HO() =>
            DataRecord[DetailedReasonType](
              namespace = None,
              key = Some("vatOutOfScope"),
              value = DetailedReasonType(
                "HO",
                "harmadik országban teljesített ügylet"
              )
            )
        }
      )

  }

}
