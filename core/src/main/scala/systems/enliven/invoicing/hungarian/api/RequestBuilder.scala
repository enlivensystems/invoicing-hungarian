package systems.enliven.invoicing.hungarian.api

import org.apache.commons.lang3.RandomStringUtils
import scalaxb.DataRecord
import scalaxb.DataRecord.__StringXMLFormat
import systems.enliven.invoicing.hungarian.core.Logger
import systems.enliven.invoicing.hungarian.generated.{
  BasicHeaderType,
  CryptoType,
  Number1u460,
  Number3u460,
  UserHeaderType
}

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import javax.xml.datatype.DatatypeFactory

class RequestBuilder(apiData: Data) extends Logger {
  private val passwordHash: String = Hash.hashSHA512(apiData.auth.password)

  def nextRequestID: String = RandomStringUtils.randomAlphanumeric(30)

  def buildBasicHeader(requestID: String, timestamp: Instant): BasicHeaderType =
    BasicHeaderType(
      requestID,
      DatatypeFactory.newInstance.newXMLGregorianCalendar(
        RequestBuilder.instantFormatter.format(timestamp)
      ),
      Number3u460.toString,
      Some(Number1u460.toString)
    )

  def buildUserHeader[T : Hash](requestID: String, timestamp: Instant, payload: T): UserHeaderType =
    UserHeaderType(
      apiData.auth.login,
      CryptoType(passwordHash, Map("@cryptoType" -> DataRecord[String](None, None, "SHA-512"))),
      apiData.entity.taxNumber,
      CryptoType(
        buildRequestSignature(requestID, timestamp, payload),
        Map("@cryptoType" -> DataRecord[String](None, None, "SHA3-512"))
      )
    )

  def buildRequestSignature[T : Hash](requestID: String, timestamp: Instant, payload: T): String =
    Hash.hashSHA3512 {
      requestID +
        RequestBuilder.signatureDateFormatter.format(timestamp) +
        apiData.auth.signingKey +
        implicitly[Hash[T]].hashed(payload)
    }

  def buildUserHeader(requestID: String, timestamp: Instant): UserHeaderType =
    UserHeaderType(
      apiData.auth.login,
      CryptoType(passwordHash, Map("@cryptoType" -> DataRecord[String](None, None, "SHA-512"))),
      apiData.entity.taxNumber,
      CryptoType(
        buildRequestSignature(requestID, timestamp),
        Map("@cryptoType" -> DataRecord[String](None, None, "SHA3-512"))
      )
    )

  def buildRequestSignature(requestID: String, timestamp: Instant): String =
    Hash.hashSHA3512 {
      requestID +
        RequestBuilder.signatureDateFormatter.format(timestamp) +
        apiData.auth.signingKey
    }

}

object RequestBuilder {

  private val instantFormatter: DateTimeFormatter =
    DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
      .withZone(ZoneId.of("UTC"))

  private val signatureDateFormatter: DateTimeFormatter =
    DateTimeFormatter
      .ofPattern("yyyyMMddHHmmss")
      .withZone(ZoneId.of("UTC"))

}
