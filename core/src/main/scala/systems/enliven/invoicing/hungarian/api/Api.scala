package systems.enliven.invoicing.hungarian.api

import java.nio.charset.Charset
import java.time.Instant

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import akka.util.ByteString
import scalaxb.{Base64Binary, XMLFormat}
import systems.enliven.invoicing.hungarian.core
import systems.enliven.invoicing.hungarian.core.{Configuration, Logger}
import systems.enliven.invoicing.hungarian.generated.{
  CREATE,
  GeneralErrorResponse,
  InvoiceOperationListType,
  InvoiceOperationType,
  MODIFY,
  ManageInvoiceRequest,
  ManageInvoiceResponse,
  ONLINE_SERVICE,
  STORNO,
  SoftwareType,
  TokenExchangeRequest,
  TokenExchangeResponse
}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.reflect.ClassTag
import scala.util.Try

class Api(signingKeyOverride: Option[String] = None)(
  implicit configuration: Configuration,
  actorSystem: ActorSystem,
  ec: ExecutionContextExecutor)
 extends Logger {

  private lazy val buildSoftware: SoftwareType =
    SoftwareType(
      apiData.software.id,
      apiData.software.name,
      ONLINE_SERVICE,
      apiData.software.version,
      apiData.software.developer.name,
      apiData.software.developer.contact,
      apiData.software.developer.countryCode,
      apiData.software.developer.taxNumber
    )

  private val apiData: Data = Data(signingKeyOverride)(configuration)
  private[hungarian] val builder: RequestBuilder = new RequestBuilder(apiData)

  def getExchangeKey: String = apiData.auth.exchangeKey

  def manageInvoice(invoiceOperations: InvoiceOperationListType)(
    implicit token: Token
  ): Future[Try[ManageInvoiceResponse]] = {
    val timestamp = Instant.now()
    val requestID: String = builder.nextRequestID

    val payload = ManageInvoiceRequest(
      builder.buildBasicHeader(requestID, timestamp),
      builder.buildUserHeader(requestID, timestamp, invoiceOperations)(
        Hash.InvoiceOperationListHash
      ),
      buildSoftware,
      token.value,
      invoiceOperations
    )

    request("manageInvoice", Api.write[ManageInvoiceRequest](payload))
      .map {
        case (status: StatusCode, response: String) =>
          Try {
            status match {
              case StatusCodes.OK =>
                Api.parse[ManageInvoiceResponse](response)
              case _ =>
                val errorResponse = Api.parse[GeneralErrorResponse](Fixer.fixResponse(response))
                throw new core.Exception(Api.format(errorResponse))
            }
          }
      }
  }

  private[hungarian] def tokenExchange(): Future[Try[TokenExchangeResponse]] = {
    val timestamp: Instant = Instant.now()
    val requestID: String = builder.nextRequestID

    val payload = TokenExchangeRequest(
      builder.buildBasicHeader(requestID, timestamp),
      builder.buildUserHeader(requestID, timestamp),
      buildSoftware
    )

    request("tokenExchange", Api.write[TokenExchangeRequest](payload))
      .map {
        case (status: StatusCode, response: String) =>
          Try {
            status match {
              case StatusCodes.OK =>
                Api.parse[TokenExchangeResponse](response)
              case _ =>
                val errorResponse = Api.parse[GeneralErrorResponse](Fixer.fixResponse(response))
                throw new core.Exception(Api.format(errorResponse))
            }
          }
      }
  }

  private def request(path: String, body: String): Future[(StatusCode, String)] = {
    val URI = apiData.request.base + path

    val contentType = ContentType(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`)

    Http().singleRequest(
      HttpRequest(
        uri = URI,
        method = HttpMethods.POST,
        headers = Accept(MediaTypes.`application/xml`) :: Nil,
        entity = HttpEntity.Strict(contentType, ByteString(body, Charset.forName("UTF-8")))
          .withContentType(contentType)
      )
    ).flatMap[(StatusCode, String)] {
      case HttpResponse(status, _, entity, _) =>
        entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(_.utf8String).map((status, _))
    }
  }

}

object Api {

  private def parse[T](data: String)(implicit format: XMLFormat[T]): T =
    scalaxb.fromXML[T](scala.xml.XML.loadString(data))

  private def write[T : ClassTag](data: T)(implicit format: XMLFormat[T]): String =
    """<?xml version="1.0" encoding="UTF-8"?>""" +
      scalaxb.toXML(
        data,
        implicitly[ClassTag[T]].runtimeClass.getSimpleName,
        scalaxb.toScope(None -> "http://schemas.nav.gov.hu/OSA/2.0/api")
      ).toString()

  private def format(err: GeneralErrorResponse): String =
    err.result.funcCode.toString +
      err.result.errorCode.map(errCode => " [" + errCode + "]").getOrElse("") +
      err.result.message.map(errMsg => " with message [" + errMsg + "]").getOrElse("") +
      err.technicalValidationMessages.map {
        validationError =>
          " with validation error [" + validationError.validationResultCode.toString + "]" +
            validationError.validationErrorCode.map(errCode => " [" + errCode + "]").getOrElse("") +
            validationError.message.map(errMsg => " with message [" + errMsg + "]").getOrElse("")
      }.mkString

  object Protocol {

    object Request {

      case class Invoices(invoices: Seq[Invoices.Invoice]) {

        protected[hungarian] def toRequest: InvoiceOperationListType =
          InvoiceOperationListType(
            compressedContent = false,
            invoices.zipWithIndex.map {
              case (invoice, index) =>
                InvoiceOperationType(
                  index + 1,
                  invoice.operation match {
                    case Invoices.Operation.create =>
                      CREATE
                    case Invoices.Operation.modify =>
                      MODIFY
                    case Invoices.Operation.storno =>
                      STORNO
                  },
                  new Base64Binary(invoice.data.toVector)
                )
            }
          )

      }

      object Invoices {

        case class Invoice(operation: Operation.Value, data: Array[Byte])

        case object Operation extends Enumeration {
          val create, modify, storno = Value
        }

      }

    }

  }

}
