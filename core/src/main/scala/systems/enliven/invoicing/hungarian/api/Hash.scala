package systems.enliven.invoicing.hungarian.api

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import org.bouncycastle.jcajce.provider.digest.SHA3
import systems.enliven.invoicing.hungarian.generated.InvoiceOperationListType

trait Hash[T] {
  def hashed(payload: T): String
}

object Hash {

  def hashSHA512(string: String): String =
    MessageDigest.getInstance("SHA-512").digest(string.getBytes(StandardCharsets.UTF_8)).map(
      "%02x".format(_)
    ).mkString.toUpperCase

  def hashSHA3512(string: String): String =
    new SHA3.Digest512().digest(string.getBytes(StandardCharsets.UTF_8)).map(
      "%02x".format(_)
    ).mkString.toUpperCase

  implicit object InvoiceOperationListHash extends Hash[InvoiceOperationListType] {

    def hashed(payload: InvoiceOperationListType): String =
      payload.invoiceOperation
        .map(invoice => invoice.invoiceOperation.toString + invoice.invoiceData)
        .map(base => hashSHA3512(base))
        .mkString

  }

}
