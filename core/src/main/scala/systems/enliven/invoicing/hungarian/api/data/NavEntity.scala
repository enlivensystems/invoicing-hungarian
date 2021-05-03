package systems.enliven.invoicing.hungarian.api.data

case class NavEntity(
  credentials: NavEntity.Credentials,
  taxNumber: String
) {
  require(taxNumber.matches("[0-9]{8}"))
}

object NavEntity {

  def create(taxNumber: String, credentials: Credentials): NavEntity =
    NavEntity(
      credentials = credentials,
      taxNumber = taxNumber
    )

  case class Credentials(
    signingKey: String,
    exchangeKey: String,
    login: String,
    password: String) {
    require(signingKey.trim.nonEmpty)
    require(exchangeKey.trim.nonEmpty)
    require(login.trim.nonEmpty)
    require(password.trim.nonEmpty)
  }

}
