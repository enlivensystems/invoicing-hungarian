package systems.enliven.invoicing.hungarian.api

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

import javax.xml.datatype.DatatypeFactory
import org.apache.commons.lang3.RandomStringUtils
import systems.enliven.invoicing.hungarian.core.Logger
import systems.enliven.invoicing.hungarian.generated.{
  BasicHeaderType,
  Number1u460Value,
  Number2u460,
  UserHeaderType
}

class RequestBuilder(apiData: Data) extends Logger {
  private val passwordHash: String = Hash.hashSHA512(apiData.auth.password)

  def nextRequestID: String =
    RandomStringUtils.randomAlphanumeric(30)

  def buildBasicHeader(requestID: String, timestamp: Instant): BasicHeaderType =
    BasicHeaderType(
      requestID,
      DatatypeFactory.newInstance.newXMLGregorianCalendar(
        RequestBuilder.instantFormatter.format(timestamp)
      ),
      Number2u460,
      Some(Number1u460Value)
    )

  def buildUserHeader[T : Hash](requestID: String, timestamp: Instant, payload: T): UserHeaderType =
    UserHeaderType(
      apiData.auth.login,
      passwordHash,
      apiData.entity.taxNumber,
      buildRequestSignature(requestID, timestamp, payload)
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
      passwordHash,
      apiData.entity.taxNumber,
      buildRequestSignature(requestID, timestamp)
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
