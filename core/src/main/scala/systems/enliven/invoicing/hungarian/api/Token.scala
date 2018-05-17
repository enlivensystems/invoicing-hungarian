package systems.enliven.invoicing.hungarian.api

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import systems.enliven.invoicing.hungarian.generated.TokenExchangeResponse

case class Token(value: String,
                 refreshed: Long,
                 validFrom: Long,
                 validTo: Long) {
  def this(tokenExchangeResponse: TokenExchangeResponse, exchangeKey: String) =
    this(
      Token.decodeToken(tokenExchangeResponse.encodedExchangeToken.vector.toArray, exchangeKey),
      System.currentTimeMillis(),
      tokenExchangeResponse.tokenValidityFrom.toGregorianCalendar.getTimeInMillis,
      tokenExchangeResponse.tokenValidityTo.toGregorianCalendar.getTimeInMillis)
}

object Token {
  private def decodeToken(token: Array[Byte], key: String): String = {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes("UTF-8"), "AES"))
    new String(cipher.doFinal(token))
  }
}
