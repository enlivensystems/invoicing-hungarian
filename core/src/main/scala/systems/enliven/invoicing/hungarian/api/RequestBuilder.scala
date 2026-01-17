package systems.enliven.invoicing.hungarian.api

import org.apache.commons.lang3.RandomStringUtils
import scalaxb.DataRecord
import scalaxb.DataRecord.__StringXMLFormat
import systems.enliven.invoicing.hungarian.api.data.Entity
import systems.enliven.invoicing.hungarian.core.Logger
import systems.enliven.invoicing.hungarian.generated.{
  BasicHeaderType, CryptoType, Number1u460, Number3u460, UserHeaderType
}

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import javax.xml.datatype.DatatypeFactory

class RequestBuilder() extends Logger {

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

  def buildUserHeader[T : Hash](
    entity: Entity,
    requestID: String,
    timestamp: Instant,
    payload: T
  ): UserHeaderType =
    UserHeaderType(
      entity.credentials.login,
      CryptoType(
        Hash.hashSHA512(entity.credentials.password),
        Map("@cryptoType" -> DataRecord[String](None, None, "SHA-512"))
      ),
      entity.taxNumber,
      CryptoType(
        buildRequestSignature(entity, requestID, timestamp, payload),
        Map("@cryptoType" -> DataRecord[String](None, None, "SHA3-512"))
      )
    )

  def buildRequestSignature[T : Hash](
    entity: Entity,
    requestID: String,
    timestamp: Instant,
    payload: T
  ): String =
    Hash.hashSHA3512 {
      requestID +
        RequestBuilder.signatureDateFormatter.format(timestamp) +
        entity.credentials.signingKey +
        implicitly[Hash[T]].hashed(payload)
    }

  def buildUserHeader(
    requestID: String,
    timestamp: Instant,
    entity: Entity
  ): UserHeaderType =
    UserHeaderType(
      entity.credentials.login,
      CryptoType(
        Hash.hashSHA512(entity.credentials.password),
        Map("@cryptoType" -> DataRecord[String](None, None, "SHA-512"))
      ),
      entity.taxNumber,
      CryptoType(
        buildRequestSignature(entity, requestID, timestamp),
        Map("@cryptoType" -> DataRecord[String](None, None, "SHA3-512"))
      )
    )

  def buildRequestSignature(
    entity: Entity,
    requestID: String,
    timestamp: Instant
  ): String =
    Hash.hashSHA3512 {
      requestID +
        RequestBuilder.signatureDateFormatter.format(timestamp) +
        entity.credentials.signingKey
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
