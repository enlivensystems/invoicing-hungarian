package systems.enliven.invoicing.hungarian.api

import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.{Date, TimeZone}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import akka.util.ByteString
import javax.xml.datatype.DatatypeFactory
import scalaxb.{Base64Binary, DataRecord, XMLFormat}
import systems.enliven.invoicing.hungarian.core
import systems.enliven.invoicing.hungarian.core.{Configuration, Logger}
import systems.enliven.invoicing.hungarian.generated.{
  AddressType,
  CARD,
  CREATE,
  CustomerInfoType,
  DetailedAddressType,
  ELECTRONICValue,
  GeneralErrorResponse,
  InvoiceDataType,
  InvoiceDetailType,
  InvoiceHeadType,
  InvoiceMainType,
  InvoiceOperationListType,
  InvoiceOperationType,
  InvoiceReferenceType,
  InvoiceType,
  LineType,
  LinesType,
  MODIFY,
  ManageInvoiceRequest,
  ManageInvoiceResponse,
  NORMAL,
  ONLINE_SERVICE,
  PIECE,
  SERVICE,
  STORNO,
  SoftwareType,
  SummaryType,
  SupplierInfoType,
  TaxNumberType,
  TokenExchangeRequest,
  TokenExchangeResponse,
  _
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

  def queryTransactionStatus(
    transactionID: String,
    returnOriginalRequest: Boolean = false
  ): Future[Try[QueryTransactionStatusResponse]] = {
    val timestamp = Instant.now()
    val requestID: String = builder.nextRequestID

    val payload = QueryTransactionStatusRequest(
      builder.buildBasicHeader(requestID, timestamp),
      builder.buildUserHeader(requestID, timestamp),
      buildSoftware,
      transactionID,
      Some(returnOriginalRequest)
    )

    request("queryTransactionStatus", Api.writeRequest[QueryTransactionStatusRequest](payload))
      .map {
        case (status: StatusCode, response: String) =>
          Try {
            status match {
              case StatusCodes.OK =>
                Api.parse[QueryTransactionStatusResponse](response)
              case _ =>
                val errorResponse = Api.parse[GeneralErrorResponse](Fixer.fixResponse(response))
                throw new core.Exception(Api.format(errorResponse))
            }
          }
      }
  }

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

    request("manageInvoice", Api.writeRequest[ManageInvoiceRequest](payload))
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

    request("tokenExchange", Api.writeRequest[TokenExchangeRequest](payload))
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
    log.trace("Initiating request to [{}] with body [{}].", path, body)

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

object Api extends XMLProtocol with Logger {

  private def parse[T](data: String)(implicit format: XMLFormat[T]): T =
    scalaxb.fromXML[T](scala.xml.XML.loadString(data))

  private def writeData[T : ClassTag](
    data: T
  )(implicit format: XMLFormat[T]): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
      scalaxb.toXML(
        data,
        implicitly[ClassTag[T]].runtimeClass.getSimpleName.stripSuffix("Type"),
        scalaxb.toScope(None -> "http://schemas.nav.gov.hu/OSA/2.0/data")
      ).toString()

  private def writeRequest[T : ClassTag](
    data: T
  )(implicit format: XMLFormat[T]): String =
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
                  invoice.base64
                )
            }
          )

      }

      object Invoices {

        trait Invoice {
          val operation: Operation.Value

          def base64: Base64Binary
        }

        case class Protocol(
          operation: Operation.Value,
          data: InvoiceDataType)
         extends Invoice {
          override def base64 = Base64Binary(Api.writeRequest(data))
        }

        case class Raw(
          operation: Operation.Value,
          data: Array[Byte])
         extends Invoice {
          override def base64 = new Base64Binary(data.toVector)
        }

        case class Smart(
          number: String,
          reference: Option[Reference],
          issued: Date,
          delivered: Date,
          paid: Date,
          currencyCode: String,
          exchangeRate: Double,
          periodical: Boolean,
          issuer: Issuer,
          recipient: Recipient,
          operation: Operation.Value,
          items: Seq[Item])
         extends Invoice {
          require(items.nonEmpty, "Invoice items may not be empty!")

          override def base64: Base64Binary = {
            val invoice = this
            val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
            val invoiceData = Api.writeData[InvoiceDataType](
              InvoiceDataType(
                invoice.number,
                DatatypeFactory.newInstance.newXMLGregorianCalendar(
                  simpleDateFormat.format(invoice.issued)
                ),
                InvoiceMainType(
                  Seq(
                    DataRecord[InvoiceType](
                      namespace = None,
                      key = Some("invoice"),
                      value = InvoiceType(
                        invoice.reference.map(
                          r => InvoiceReferenceType(r.number, r.reported, r.index)
                        ),
                        invoiceHead = InvoiceHeadType(
                          supplierInfo = SupplierInfoType(
                            TaxNumberType(
                              invoice.issuer.taxNumber,
                              Some(invoice.issuer.taxCode),
                              Some(invoice.issuer.taxCountry)
                            ),
                            None,
                            Some(invoice.issuer.communityTaxNumber),
                            invoice.issuer.name,
                            AddressType(
                              DataRecord[DetailedAddressType](
                                namespace = None,
                                key =
                                  Some("detailedAddress"),
                                value = DetailedAddressType(
                                  invoice.issuer.address.countryCode,
                                  invoice.issuer.address.region,
                                  invoice.issuer.address.postalCode,
                                  invoice.issuer.address.city,
                                  invoice.issuer.address.streetName,
                                  invoice.issuer.address.publicPlaceCategory,
                                  invoice.issuer.address.number,
                                  invoice.issuer.address.building,
                                  invoice.issuer.address.staircase,
                                  invoice.issuer.address.floor,
                                  invoice.issuer.address.door,
                                  invoice.issuer.address.identifier
                                )
                              )
                            ),
                            Some(invoice.issuer.bankAccountNumber),
                            Some(false),
                            None
                          ),
                          customerInfo = Option(CustomerInfoType(
                            customerTaxNumber = invoice.recipient.taxNumber.map {
                              taxNumber =>
                                TaxNumberType(
                                  taxNumber,
                                  None,
                                  None
                                )
                            },
                            groupMemberTaxNumber = None,
                            communityVatNumber = invoice.recipient.communityTaxNumber,
                            thirdStateTaxId = invoice.recipient.thirdStateTaxNumber,
                            customerName = invoice.recipient.name,
                            customerAddress = AddressType(
                              DataRecord[DetailedAddressType](
                                namespace = None,
                                key =
                                  Some("detailedAddress"),
                                value = DetailedAddressType(
                                  invoice.recipient.address.countryCode,
                                  invoice.recipient.address.region,
                                  invoice.recipient.address.postalCode,
                                  invoice.recipient.address.city,
                                  invoice.recipient.address.streetName,
                                  invoice.recipient.address.publicPlaceCategory,
                                  invoice.recipient.address.number,
                                  invoice.recipient.address.building,
                                  invoice.recipient.address.staircase,
                                  invoice.recipient.address.floor,
                                  invoice.recipient.address.door,
                                  invoice.recipient.address.identifier
                                )
                              )
                            ),
                            customerBankAccountNumber = None
                          )),
                          None,
                          InvoiceDetailType(
                            invoiceCategory = NORMAL,
                            invoiceDeliveryDate =
                              DatatypeFactory.newInstance.newXMLGregorianCalendar(
                                simpleDateFormat.format(invoice.delivered)
                              ),
                            invoiceDeliveryPeriodStart = None,
                            invoiceDeliveryPeriodEnd = None,
                            invoiceAccountingDeliveryDate = None,
                            periodicalSettlement = Some(invoice.periodical),
                            smallBusinessIndicator = None,
                            currencyCode = invoice.currencyCode,
                            exchangeRate = BigDecimal(invoice.exchangeRate),
                            selfBillingIndicator = None,
                            paymentMethod = Some(CARD),
                            paymentDate = Some(DatatypeFactory.newInstance.newXMLGregorianCalendar(
                              simpleDateFormat.format(invoice.paid)
                            )),
                            cashAccountingIndicator = None,
                            invoiceAppearance = ELECTRONICValue,
                            electronicInvoiceHash = None,
                            additionalInvoiceData = Seq.empty
                          )
                        ),
                        invoiceLines = Some {
                          var i = 0
                          LinesType(invoice.items.map {
                            item =>
                              i = i + 1
                              LineType(
                                lineNumber = BigInt(i),
                                lineModificationReference = None,
                                referencesToOtherLines = None,
                                advanceIndicator = None,
                                productCodes = None,
                                lineExpressionIndicator = false,
                                lineNatureIndicator = Some(SERVICE),
                                lineDescription = Some(item.name),
                                quantity = Some(BigDecimal(item.quantity)),
                                unitOfMeasure = Some(PIECE),
                                unitOfMeasureOwn = None,
                                unitPrice = Some(BigDecimal(item.price)),
                                unitPriceHUF = None,
                                lineDiscountData = None,
                                linetypeoption = None,
                                intermediatedService = Some(item.intermediated),
                                aggregateInvoiceLineData = None,
                                newTransportMean = None,
                                depositIndicator = Some(false),
                                marginSchemeIndicator = None,
                                ekaerIds = None,
                                obligatedForProductFee = None,
                                GPCExcise = None,
                                dieselOilPurchase = None,
                                netaDeclaration = None,
                                productFeeClause = None,
                                lineProductFeeContent = Seq.empty,
                                additionalLineData = Seq.empty
                              )
                          })
                        },
                        productFeeSummary = Seq.empty,
                        invoiceSummary = SummaryType(
                          Seq(DataRecord[SummaryNormalType](
                            namespace = None,
                            key = Some("summaryNormal"),
                            SummaryNormalType(
                              invoice.items.groupBy(_.tax).map {
                                case (rate, items) =>
                                  val netInCurrency =
                                    items.map(item => item.price * item.quantity).sum
                                  val netInHUF = items.map(
                                    item => item.price * item.quantity
                                  ).sum * invoice.exchangeRate
                                  val taxInCurrency =
                                    items.map(item => item.price * item.quantity * item.tax).sum
                                  val taxInHUF = items.map(
                                    item => item.price * item.quantity * item.tax
                                  ).sum * invoice.exchangeRate
                                  SummaryByVatRateType(
                                    VatRateType(
                                      DataRecord[BigDecimal](
                                        namespace = None,
                                        key = Some("vatPercentage"),
                                        BigDecimal(rate)
                                      )
                                    ),
                                    VatRateNetDataType(
                                      BigDecimal(netInCurrency),
                                      BigDecimal(netInHUF)
                                    ),
                                    VatRateVatDataType(
                                      BigDecimal(taxInCurrency),
                                      BigDecimal(taxInHUF)
                                    ),
                                    Some(VatRateGrossDataType(
                                      BigDecimal(netInCurrency + taxInCurrency),
                                      BigDecimal(netInHUF + taxInHUF)
                                    ))
                                  )
                              }.toSeq,
                              BigDecimal(1.0),
                              BigDecimal(1.0),
                              BigDecimal(1.0),
                              BigDecimal(1.0)
                            )
                          )),
                          None
                        )
                      )
                    )
                  )
                )
              )
            )
            log.trace("Invoice XML data before base-64 is [{}].", invoiceData)
            new Base64Binary(invoiceData.getBytes("UTF-8").toVector)
          }

        }

        case class Recipient(
          taxNumber: Option[String] = None,
          taxCountry: Option[String] = None,
          communityTaxNumber: Option[String] = None,
          thirdStateTaxNumber: Option[String] = None,
          name: String,
          address: Address,
          bankAccountNumber: String) {
          require(taxNumber.forall(_.matches("""[0-9]{8}""")))
          require(taxCountry.forall(_.matches("""[0-9]{2}""")))
          require(communityTaxNumber.forall(_.matches("""[A-Z]{2}[0-9A-Z]{2,13}""")))
          require(name.nonEmpty)
          require(bankAccountNumber.matches(
            """[0-9]{8}[-][0-9]{8}[-][0-9]{8}|[0-9]{8}[-][0-9]{8}|[A-Z]{2}[0-9]{2}[0-9A-Za-z]{11,30}"""
          ))
        }

        case class Address(
          countryCode: String,
          region: Option[String] = None,
          postalCode: String,
          city: String,
          streetName: String,
          publicPlaceCategory: String,
          number: Option[String] = None,
          building: Option[String] = None,
          staircase: Option[String] = None,
          floor: Option[String] = None,
          door: Option[String] = None,
          identifier: Option[String] = None) {
          require(countryCode.matches("""[A-Z]{2}"""))
          require(postalCode.matches("""[A-Z0-9][A-Z0-9\s\-]{1,8}[A-Z0-9]"""))
        }

        case class Issuer(
          taxNumber: String,
          taxCode: String,
          taxCountry: String,
          communityTaxNumber: String,
          name: String,
          address: Address,
          bankAccountNumber: String) {
          require(taxCode.matches("""[1-5]"""))
          require(taxCountry.matches("""[0-9]{2}"""))
          require(communityTaxNumber.matches("""[A-Z]{2}[0-9A-Z]{2,13}"""))
          require(name.nonEmpty)
          require(bankAccountNumber.nonEmpty)
        }

        case class Item(
          name: String,
          quantity: Int,
          price: Double,
          tax: Double,
          intermediated: Boolean)

        case class Reference(number: String, reported: Boolean = true, index: Int = 1)

        case object Operation extends Enumeration {
          val create, modify, storno = Value
        }

      }

    }

  }

}
