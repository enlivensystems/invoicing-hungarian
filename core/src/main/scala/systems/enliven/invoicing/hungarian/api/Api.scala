package systems.enliven.invoicing.hungarian.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import akka.util.ByteString
import scalaxb.{Base64Binary, DataRecord, XMLFormat}
import systems.enliven.invoicing.hungarian.api.data.{Issuer, Item}
import systems.enliven.invoicing.hungarian.api.recipient.Recipient
import systems.enliven.invoicing.hungarian.core
import systems.enliven.invoicing.hungarian.core.{Configuration, Logger}
import systems.enliven.invoicing.hungarian.generated.{
  AddressType,
  CARD,
  CREATE,
  DetailedAddressType,
  GeneralErrorResponse,
  InvoiceDataType,
  InvoiceDetailType,
  InvoiceHeadType,
  InvoiceMainType,
  InvoiceOperationListType,
  InvoiceOperationType,
  InvoiceReferenceType,
  InvoiceType,
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

import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.{Date, TimeZone}
import javax.xml.datatype.DatatypeFactory
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
      header = builder.buildBasicHeader(requestID, timestamp),
      user = builder.buildUserHeader(requestID, timestamp),
      software = buildSoftware
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
    """<?xml version="1.0" encoding="UTF-8"?>""" + scalaxb.toXML(
      data,
      implicitly[ClassTag[T]].runtimeClass.getSimpleName.stripSuffix("Type"),
      scalaxb.toScope(
        None -> "http://schemas.nav.gov.hu/OSA/3.0/data",
        Some("common") -> "http://schemas.nav.gov.hu/NTCA/1.0/common",
        Some("base") -> "http://schemas.nav.gov.hu/OSA/3.0/base"
      )
    ).toString()

  private def writeRequest[T : ClassTag](
    data: T
  )(implicit format: XMLFormat[T]): String =
    """<?xml version="1.0" encoding="UTF-8"?>""" +
      scalaxb.toXML(
        data,
        implicitly[ClassTag[T]].runtimeClass.getSimpleName,
        scalaxb.toScope(
          None -> "http://schemas.nav.gov.hu/OSA/3.0/api",
          Some("common") -> "http://schemas.nav.gov.hu/NTCA/1.0/common",
          Some("base") -> "http://schemas.nav.gov.hu/OSA/3.0/base"
        )
      ).toString()

  private def format(err: GeneralErrorResponse): String =
    err.result.funcCode.toString +
      err.result.errorCode.map(
        errCode => " [" + errCode + "]"
      ).getOrElse("") +
      err.result.message.map(
        errMsg => " with message [" + errMsg + "]"
      ).getOrElse("") +
      err.technicalValidationMessages.map {
        validationError =>
          " with validation error [" + validationError.validationResultCode.toString + "]" +
            validationError.validationErrorCode.map(
              errCode => " [" + errCode + "]"
            ).getOrElse("") +
            validationError.message.map(
              errMsg => " with message [" + errMsg + "]"
            ).getOrElse("")
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
          override def base64: Base64Binary = Base64Binary(Api.writeRequest(data))
        }

        case class Raw(
          operation: Operation.Value,
          data: Array[Byte])
         extends Invoice {
          override def base64: Base64Binary = new Base64Binary(data.toVector)
        }

        case class Smart(
          number: String,
          reference: Option[Reference],
          issued: Date,
          delivered: Date,
          paid: Date,
          currencyCode: String,
          exchangeRate: BigDecimal,
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
                completenessIndicator = false,
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
                              taxpayerId = invoice.issuer.taxNumber,
                              vatCode = Some(invoice.issuer.taxCode),
                              countyCode = Some(invoice.issuer.taxCounty)
                            ),
                            None,
                            Some(invoice.issuer.communityTaxNumber),
                            invoice.issuer.name,
                            AddressType(
                              DataRecord[DetailedAddressType](
                                namespace = Some("http://schemas.nav.gov.hu/OSA/3.0/base"),
                                key = Some("detailedAddress"),
                                value = DetailedAddressType(
                                  countryCode = invoice.issuer.address.countryCode,
                                  region = invoice.issuer.address.region,
                                  postalCode = invoice.issuer.address.postalCode,
                                  city = invoice.issuer.address.city,
                                  streetName = invoice.issuer.address.streetName,
                                  publicPlaceCategory = invoice.issuer.address.publicPlaceCategory,
                                  number = invoice.issuer.address.number,
                                  building = invoice.issuer.address.building,
                                  staircase = invoice.issuer.address.staircase,
                                  floor = invoice.issuer.address.floor,
                                  door = invoice.issuer.address.door,
                                  lotNumber = invoice.issuer.address.lotNumber
                                )
                              )
                            ),
                            Some(invoice.issuer.bankAccountNumber),
                            Some(false),
                            None
                          ),
                          customerInfo = Some(recipient.toCustomerInfoType),
                          fiscalRepresentativeInfo = None,
                          invoiceDetail = InvoiceDetailType(
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
                            exchangeRate = invoice.exchangeRate,
                            selfBillingIndicator = None,
                            paymentMethod = Some(CARD),
                            paymentDate = Some(DatatypeFactory.newInstance.newXMLGregorianCalendar(
                              simpleDateFormat.format(invoice.paid)
                            )),
                            cashAccountingIndicator = None,
                            invoiceAppearance = ELECTRONIC,
                            conventionalInvoiceInfo = None,
                            additionalInvoiceData = Seq.empty
                          )
                        ),
                        invoiceLines = Some {
                          var i = 0
                          LinesType(
                            mergedItemIndicator = false,
                            invoice.items.map {
                              item =>
                                i = i + 1
                                val unitPrice: BigDecimal =
                                  item.price.setScale(2, BigDecimal.RoundingMode.HALF_UP)

                                val unitPriceHUF: BigDecimal = (unitPrice * invoice.exchangeRate)
                                  .setScale(2, BigDecimal.RoundingMode.HALF_UP)

                                val lineNetAmount: BigDecimal = unitPrice * item.quantity
                                val lineNetAmountHUF: BigDecimal = unitPriceHUF * item.quantity

                                val lineVatAmount: BigDecimal = (lineNetAmount * item.tax).setScale(
                                  2,
                                  BigDecimal.RoundingMode.HALF_UP
                                )
                                val lineVatAmountHUF: BigDecimal =
                                  (lineNetAmountHUF * item.tax).setScale(
                                    2,
                                    BigDecimal.RoundingMode.HALF_UP
                                  )

                                LineType(
                                  lineNumber = BigInt(i),
                                  lineModificationReference = None,
                                  referencesToOtherLines = None,
                                  advanceData = None,
                                  productCodes = None,
                                  lineExpressionIndicator = false,
                                  lineNatureIndicator = Some(SERVICE),
                                  lineDescription = Some(item.name),
                                  quantity = Some(BigDecimal(item.quantity)),
                                  unitOfMeasure = Some(PIECE),
                                  unitOfMeasureOwn = None,
                                  unitPrice = Some(unitPrice),
                                  unitPriceHUF = Some(unitPriceHUF),
                                  lineDiscountData = None,
                                  linetypeoption = Some(
                                    DataRecord[LineAmountsNormalType](
                                      namespace = None,
                                      key = Some("lineAmountsNormal"),
                                      value = LineAmountsNormalType(
                                        lineNetAmountData = LineNetAmountDataType(
                                          lineNetAmount = lineNetAmount,
                                          lineNetAmountHUF = lineNetAmountHUF
                                        ),
                                        lineVatRate = VatRateType(
                                          DataRecord[BigDecimal](
                                            namespace = None,
                                            key = Some("vatPercentage"),
                                            item.tax.setScale(2, BigDecimal.RoundingMode.HALF_UP)
                                          )
                                        ),
                                        lineVatData = Some(LineVatDataType(
                                          lineVatAmount = lineVatAmount,
                                          lineVatAmountHUF = lineVatAmountHUF
                                        )),
                                        lineGrossAmountData = Some(
                                          LineGrossAmountDataType(
                                            lineGrossAmountNormal = lineNetAmount + lineVatAmount,
                                            lineGrossAmountNormalHUF =
                                              lineNetAmountHUF + lineVatAmountHUF
                                          )
                                        )
                                      )
                                    )
                                  ),
                                  intermediatedService = Some(item.intermediated),
                                  aggregateInvoiceLineData = None,
                                  newTransportMean = None,
                                  depositIndicator = Some(false),
                                  obligatedForProductFee = None,
                                  GPCExcise = None,
                                  dieselOilPurchase = None,
                                  netaDeclaration = None,
                                  productFeeClause = None,
                                  lineProductFeeContent = Seq.empty,
                                  conventionalLineInfo = None,
                                  additionalLineData = Seq.empty
                                )
                            }
                          )
                        },
                        productFeeSummary = Seq.empty,
                        invoiceSummary = SummaryType(
                          Seq(DataRecord[SummaryNormalType](
                            namespace = None,
                            key = Some("summaryNormal"), {
                              val taxRateSummary = invoice.items.groupBy(_.tax).map {
                                case (rate, items) =>
                                  val netInCurrency =
                                    items.map(
                                      item => item.price * item.quantity
                                    ).sum.setScale(2, BigDecimal.RoundingMode.HALF_UP)
                                  val netInHUF = (items.map(
                                    item => item.price * item.quantity
                                  ).sum * invoice.exchangeRate)
                                    .setScale(2, BigDecimal.RoundingMode.HALF_UP)
                                  val taxInCurrency =
                                    items.map(
                                      item => item.price * item.quantity * item.tax
                                    ).sum.setScale(2, BigDecimal.RoundingMode.HALF_UP)
                                  val taxInHUF = (items.map(
                                    item => item.price * item.quantity * item.tax
                                  ).sum * invoice.exchangeRate)
                                    .setScale(2, BigDecimal.RoundingMode.HALF_UP)
                                  rate -> (netInCurrency, netInHUF, taxInCurrency, taxInHUF)
                              }
                              SummaryNormalType(
                                taxRateSummary.map {
                                  case (rate, (netInCurrency, netInHUF, taxInCurrency, taxInHUF)) =>
                                    SummaryByVatRateType(
                                      VatRateType(
                                        DataRecord[BigDecimal](
                                          namespace = None,
                                          key = Some("vatPercentage"),
                                          rate.setScale(2, BigDecimal.RoundingMode.HALF_UP)
                                        )
                                      ),
                                      VatRateNetDataType(
                                        netInCurrency,
                                        netInHUF
                                      ),
                                      VatRateVatDataType(
                                        taxInCurrency,
                                        taxInHUF
                                      ),
                                      Some(VatRateGrossDataType(
                                        netInCurrency + taxInCurrency,
                                        netInHUF + taxInHUF
                                      ))
                                    )
                                }.toSeq,
                                taxRateSummary.map(_._2._1).sum,
                                taxRateSummary.map(_._2._2).sum,
                                taxRateSummary.map(_._2._3).sum,
                                taxRateSummary.map(_._2._4).sum
                              )
                            }
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

        case class Reference(number: String, reported: Boolean = true, index: Int = 1)

        case object Operation extends Enumeration {
          val create, modify, storno = Value
        }

      }

    }

  }

}
