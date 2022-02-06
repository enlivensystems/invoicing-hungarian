package systems.enliven.invoicing.hungarian.api.data

import systems.enliven.invoicing.hungarian.generated.QueryTaxpayerResponse

case class Taxpayer(
  identifier: String,
  name: String,
  shortName: Option[String],
  incorporation: String,
  validity: Taxpayer.Validity.Value,
  vatCode: Option[String],
  countyCode: Option[String],
  vatGroupMembership: Option[String],
  addresses: Seq[(String, Address)]
)

object Taxpayer {

  object Validity extends Enumeration {
    val VALID, INVALID, UNKNOWN = Value
  }

  def create(response: QueryTaxpayerResponse): Taxpayer =
    Taxpayer(
      identifier = response.taxpayerData.get.taxNumberDetail.taxpayerId,
      name = response.taxpayerData.get.taxpayerName,
      shortName = response.taxpayerData.get.taxpayerShortName,
      incorporation = response.taxpayerData.get.incorporation.toString,
      validity = response.taxpayerValidity
        .map(if (_) Validity.VALID else Validity.INVALID).getOrElse(Validity.UNKNOWN),
      vatCode = response.taxpayerData.get.taxNumberDetail.vatCode,
      countyCode = response.taxpayerData.get.taxNumberDetail.countyCode,
      vatGroupMembership = response.taxpayerData.get.vatGroupMembership,
      addresses = response.taxpayerData.get.taxpayerAddressList
        .map(_.taxpayerAddressItem)
        .getOrElse(Nil)
        .map(address =>
          (address.taxpayerAddressType.toString, Address.create(address.taxpayerAddress))
        )
    )

}
