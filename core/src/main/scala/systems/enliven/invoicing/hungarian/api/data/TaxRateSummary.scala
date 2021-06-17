package systems.enliven.invoicing.hungarian.api.data

final case class TaxRateSummary(
  vat: VAT,
  netInCurrency: BigDecimal,
  netInHUF: BigDecimal,
  vatInCurrency: BigDecimal,
  vatInHUF: BigDecimal,
  grossInCurrency: BigDecimal,
  grossInHUF: BigDecimal
)
