package systems.enliven.invoicing.hungarian.api.data

case class Item(
  name: String,
  netUnitPrice: BigDecimal,
  vatUnitPrice: BigDecimal,
  grossUnitPrice: BigDecimal,
  quantity: Int,
  vat: VAT,
  intermediated: Boolean) {
  require(netUnitPrice.scale == 0)
  require(vatUnitPrice.scale == 0)
  require(grossUnitPrice.scale == 0)
  require(netUnitPrice + vatUnitPrice == grossUnitPrice)
  require(quantity == 1)

  def netPrice: BigDecimal = netUnitPrice * BigDecimal(quantity)
  def vatPrice: BigDecimal = vatUnitPrice * BigDecimal(quantity)
  def grossPrice: BigDecimal = grossUnitPrice * BigDecimal(quantity)

  def netUnitPriceHUF(exchangeRate: BigDecimal): BigDecimal =
    (netUnitPrice * exchangeRate).setScale(2, BigDecimal.RoundingMode.HALF_UP)

  def netPriceHUF(exchangeRate: BigDecimal): BigDecimal =
    (netPrice * exchangeRate).setScale(2, BigDecimal.RoundingMode.HALF_UP)

  def vatPriceHUF(exchangeRate: BigDecimal): BigDecimal =
    (vatPrice * exchangeRate).setScale(2, BigDecimal.RoundingMode.HALF_UP)

  def grossPriceHUF(exchangeRate: BigDecimal): BigDecimal =
    (grossPrice * exchangeRate).setScale(2, BigDecimal.RoundingMode.HALF_UP)

}
