package systems.enliven.invoicing.hungarian.api

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import akka.stream.RestartSettings
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import akka.util.ByteString
import scalaxb.{Base64Binary, DataRecord, XMLFormat}
import systems.enliven.invoicing.hungarian.api.Api.simpleDateFormat
import systems.enliven.invoicing.hungarian.api.data.{Entity, Issuer, Item, TaxRateSummary}
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
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.reflect.ClassTag
import scala.util.Try

class Api(
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

  private val apiData: Data = Data()
  private[hungarian] val builder: RequestBuilder = new RequestBuilder

  def queryInvoiceData(
    invoiceNumber: String,
    entity: Entity
  ): Future[Try[QueryInvoiceDataResponse]] = {
    val timestamp = Instant.now()
    val requestID: String = builder.nextRequestID

    val payload = QueryInvoiceDataRequest(
      builder.buildBasicHeader(requestID, timestamp),
      builder.buildUserHeader(requestID, timestamp, entity),
      buildSoftware,
      InvoiceNumberQueryType(invoiceNumber, OUTBOUND)
    )

    request("queryInvoiceData", Api.writeRequest[QueryInvoiceDataRequest](payload))
      .map {
        case (status: StatusCode, response: String) =>
          Try {
            status match {
              case StatusCodes.OK =>
                Api.parse[QueryInvoiceDataResponse](response)
              case _ =>
                val errorResponse = Api.parse[GeneralErrorResponse](Fixer.fixResponse(response))
                throw new core.Exception(Api.format(errorResponse))
            }
          }
      }
  }

  def queryTaxpayer(
    taxNumber: String,
    entity: Entity
  ): Future[Try[QueryTaxpayerResponse]] = {
    val timestamp = Instant.now()
    val requestID: String = builder.nextRequestID

    val payload = QueryTaxpayerRequest(
      builder.buildBasicHeader(requestID, timestamp),
      builder.buildUserHeader(requestID, timestamp, entity),
      buildSoftware,
      taxNumber
    )

    request("queryTaxpayer", Api.writeRequest[QueryTaxpayerRequest](payload))
      .map {
        case (status: StatusCode, response: String) =>
          Try {
            status match {
              case StatusCodes.OK =>
                Api.parse[QueryTaxpayerResponse](response)
              case _ =>
                val errorResponse = Api.parse[GeneralErrorResponse](Fixer.fixResponse(response))
                throw new core.Exception(Api.format(errorResponse))
            }
          }
      }
  }

  def queryInvoiceDigest(
    entity: Entity,
    page: Int = 1,
    direction: InvoiceDirectionType,
    fromDate: Date,
    toDate: Date
  ): Future[Try[QueryInvoiceDigestResponse]] = {
    require(page > 0)

    val timestamp = Instant.now()
    val requestID: String = builder.nextRequestID

    val payload = QueryInvoiceDigestRequest(
      builder.buildBasicHeader(requestID, timestamp),
      builder.buildUserHeader(requestID, timestamp, entity),
      buildSoftware,
      page,
      direction,
      InvoiceQueryParamsType(
        MandatoryQueryParamsType(
          DataRecord[DateIntervalParamType](
            namespace = None,
            key = Some("invoiceIssueDate"),
            value = DateIntervalParamType(
              DatatypeFactory.newInstance
                .newXMLGregorianCalendar(simpleDateFormat.format(fromDate)),
              DatatypeFactory.newInstance
                .newXMLGregorianCalendar(simpleDateFormat.format(toDate))
            )
          )
        )
      )
    )

    request("queryInvoiceDigest", Api.writeRequest[QueryInvoiceDigestRequest](payload))
      .map {
        case (status: StatusCode, response: String) =>
          Try {
            status match {
              case StatusCodes.OK =>
                Api.parse[QueryInvoiceDigestResponse](response)
              case _ =>
                val errorResponse = Api.parse[GeneralErrorResponse](Fixer.fixResponse(response))
                throw new core.Exception(Api.format(errorResponse))
            }
          }
      }
  }

  def queryTransactionStatus(
    transactionID: String,
    entity: Entity,
    returnOriginalRequest: Boolean = false
  ): Future[Try[QueryTransactionStatusResponse]] = {
    val timestamp = Instant.now()
    val requestID: String = builder.nextRequestID

    val payload = QueryTransactionStatusRequest(
      builder.buildBasicHeader(requestID, timestamp),
      builder.buildUserHeader(requestID, timestamp, entity),
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

  def manageInvoice(entity: Entity, invoiceOperations: InvoiceOperationListType)(
    implicit token: Token
  ): Future[Try[ManageInvoiceResponse]] = {
    val timestamp = Instant.now()
    val requestID: String = builder.nextRequestID

    val payload = ManageInvoiceRequest(
      builder.buildBasicHeader(requestID, timestamp),
      builder.buildUserHeader(entity, requestID, timestamp, invoiceOperations)(
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

  private[hungarian] def tokenExchange(entity: Entity): Future[Try[TokenExchangeResponse]] = {
    val timestamp: Instant = Instant.now()
    val requestID: String = builder.nextRequestID

    val payload = TokenExchangeRequest(
      header = builder.buildBasicHeader(requestID, timestamp),
      user = builder.buildUserHeader(requestID, timestamp, entity),
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

    val settings = RestartSettings(
      minBackoff = 3.seconds,
      maxBackoff = 30.seconds,
      randomFactor = 0.2
    ).withMaxRestarts(20, 5.minutes)

    RestartSource.withBackoff(settings) {
      () =>
        Source.futureSource[(StatusCode, String), NotUsed] {
          Http().singleRequest(
            HttpRequest(
              uri = URI,
              method = HttpMethods.POST,
              headers = Accept(MediaTypes.`application/xml`) :: Nil,
              entity = HttpEntity.Strict(contentType, ByteString(body, Charset.forName("UTF-8")))
                .withContentType(contentType)
            )
          ).flatMap[Source[(StatusCode, String), NotUsed]] {
            case HttpResponse(status, _, entity, _) =>
              entity.dataBytes
                .runFold(ByteString(""))(_ ++ _).map(_.utf8String)
                .map((status, _))
                .map(Source.single)
          }
        }
    }.runWith(Sink.head)
  }

}

object Api extends XMLProtocol with Logger {

  protected[api] val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
  simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))

  protected[hungarian] def parse[T](data: String)(implicit format: XMLFormat[T]): T =
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

            val invoiceData = Api.writeData[InvoiceDataType](
              InvoiceDataType(
                invoice.number,
                invoiceIssueDate = DatatypeFactory.newInstance
                  .newXMLGregorianCalendar(simpleDateFormat.format(invoice.issued)),
                completenessIndicator = false,
                InvoiceMainType(
                  Seq(
                    DataRecord[InvoiceType](
                      namespace = None,
                      key = Some("invoice"),
                      value = InvoiceType(
                        invoice.reference.map(r =>
                          InvoiceReferenceType(r.number, r.reported, r.index)
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
                            None,
                            None
                          ),
                          customerInfo = Some(recipient.toCustomerInfoType),
                          fiscalRepresentativeInfo = None,
                          invoiceDetail = InvoiceDetailType(
                            invoiceCategory = NORMAL,
                            invoiceDeliveryDate = DatatypeFactory.newInstance
                              .newXMLGregorianCalendar(simpleDateFormat.format(invoice.delivered)),
                            invoiceDeliveryPeriodStart = None,
                            invoiceDeliveryPeriodEnd = None,
                            invoiceAccountingDeliveryDate = None,
                            periodicalSettlement = Some(invoice.periodical),
                            smallBusinessIndicator = Some(invoice.issuer.smallBusiness),
                            currencyCode = invoice.currencyCode,
                            exchangeRate = invoice.exchangeRate,
                            selfBillingIndicator = None,
                            paymentMethod = Some(CARD),
                            paymentDate = Some(DatatypeFactory.newInstance
                              .newXMLGregorianCalendar(simpleDateFormat.format(invoice.paid))),
                            cashAccountingIndicator = Some(invoice.issuer.cashSettlement),
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
                                  unitPrice = Some(item.netUnitPrice),
                                  unitPriceHUF = Some(item.netUnitPriceHUF(invoice.exchangeRate)),
                                  lineDiscountData = None,
                                  linetypeoption = Some(
                                    DataRecord[LineAmountsNormalType](
                                      namespace = None,
                                      key = Some("lineAmountsNormal"),
                                      value = LineAmountsNormalType(
                                        lineNetAmountData = LineNetAmountDataType(
                                          lineNetAmount = item.netPrice,
                                          lineNetAmountHUF = item.netPriceHUF(invoice.exchangeRate)
                                        ),
                                        lineVatRate = item.vat.toVatRate,
                                        lineVatData = Some(LineVatDataType(
                                          lineVatAmount = item.vatPrice,
                                          lineVatAmountHUF = item.vatPriceHUF(invoice.exchangeRate)
                                        )),
                                        lineGrossAmountData = Some(LineGrossAmountDataType(
                                          lineGrossAmountNormal = item.grossPrice,
                                          lineGrossAmountNormalHUF =
                                            item.grossPriceHUF(invoice.exchangeRate)
                                        ))
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
                        invoiceSummary = {
                          val taxRateSummary = invoice.items.groupBy(_.vat).map {
                            case (vat, items) =>
                              TaxRateSummary(
                                vat = vat,
                                netInCurrency = items.map(_.netPrice).sum,
                                netInHUF = items.map(_.netPriceHUF(invoice.exchangeRate)).sum,
                                vatInCurrency = items.map(_.vatPrice).sum,
                                vatInHUF = items.map(_.vatPriceHUF(invoice.exchangeRate)).sum,
                                grossInCurrency = items.map(_.grossPrice).sum,
                                grossInHUF =
                                  items.map(_.grossPriceHUF(invoice.exchangeRate)).sum
                              )
                          }

                          SummaryType(
                            summarytypeoption = Seq(DataRecord[SummaryNormalType](
                              namespace = None,
                              key = Some("summaryNormal"),
                              value = SummaryNormalType(
                                summaryByVatRate =
                                  taxRateSummary.map {
                                    taxRateSummary =>
                                      SummaryByVatRateType(
                                        vatRate = taxRateSummary.vat.toVatRate,
                                        vatRateNetData = VatRateNetDataType(
                                          vatRateNetAmount = taxRateSummary.netInCurrency,
                                          vatRateNetAmountHUF = taxRateSummary.netInHUF
                                        ),
                                        vatRateVatData = VatRateVatDataType(
                                          vatRateVatAmount = taxRateSummary.vatInCurrency,
                                          vatRateVatAmountHUF = taxRateSummary.vatInHUF
                                        ),
                                        vatRateGrossData = Some(VatRateGrossDataType(
                                          vatRateGrossAmount = taxRateSummary.grossInCurrency,
                                          vatRateGrossAmountHUF = taxRateSummary.grossInHUF
                                        ))
                                      )
                                  }.toSeq,
                                invoiceNetAmount = taxRateSummary.map(_.netInCurrency).sum,
                                invoiceNetAmountHUF = taxRateSummary.map(_.netInHUF).sum,
                                invoiceVatAmount = taxRateSummary.map(_.vatInCurrency).sum,
                                invoiceVatAmountHUF = taxRateSummary.map(_.vatInHUF).sum
                              )
                            )),
                            summaryGrossData = Some(
                              SummaryGrossDataType(
                                invoiceGrossAmount = taxRateSummary.map(_.grossInCurrency).sum,
                                invoiceGrossAmountHUF = taxRateSummary.map(_.grossInHUF).sum
                              )
                            )
                          )
                        }
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
