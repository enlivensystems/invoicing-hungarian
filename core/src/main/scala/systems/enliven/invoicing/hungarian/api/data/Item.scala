package systems.enliven.invoicing.hungarian.api.data

case class Item(
  name: String,
  quantity: Int,
  price: BigDecimal,
  tax: BigDecimal,
  intermediated: Boolean)
