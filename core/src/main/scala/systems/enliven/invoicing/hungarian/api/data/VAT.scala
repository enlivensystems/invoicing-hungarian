package systems.enliven.invoicing.hungarian.api.data

import scalaxb.DataRecord
import systems.enliven.invoicing.hungarian.generated.{VatRateType, _}

sealed abstract class VAT(val rate: Int)

object VAT {
  final case object Standard extends VAT(27)

  /**
    * Alanyi adómentesség
    */
  final case object AAM extends VAT(0)

  /**
    * Az ÁFA területi hatályán kívül
    */
  final case object TEHK extends VAT(0)

  final val vats: Seq[VAT] = Seq(Standard, AAM, TEHK)

  def test: VAT = vats(scala.util.Random.nextInt(vats.size))

  def withName(name: String): VAT =
    vats.find(_.toString == name)
      .getOrElse(throw new NoSuchElementException(s"No value found for [$name]"))

  implicit class APIConvert(vat: VAT) {

    def toVatRate: VatRateType =
      VatRateType(
        vat match {
          case Standard =>
            DataRecord[BigDecimal](
              namespace = None,
              key = Some("vatPercentage"),
              value = BigDecimal(Standard.rate) / BigDecimal(100)
            )
          case AAM =>
            DataRecord[DetailedReasonType](
              namespace = None,
              key = Some("vatExemption"),
              value = DetailedReasonType("AAM", "alanyi adómentes")
            )
          case TEHK =>
            DataRecord[DetailedReasonType](
              namespace = None,
              key = Some("vatOutOfScope"),
              value = DetailedReasonType("TEHK", "áfa területi hatályán kívül")
            )
        }
      )

  }

}
