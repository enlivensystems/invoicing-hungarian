package systems.enliven.invoicing.hungarian

import java.io.{FileInputStream, InputStream}
import java.nio.file.{FileSystems, Files, Paths}
import java.util.Collections

import scalaxb.XMLFormat
import systems.enliven.invoicing.hungarian.generated.{InvoiceData, ManageAnnulmentRequest, ManageInvoiceRequest, QueryInvoiceChainDigestRequest, QueryInvoiceCheckRequest, QueryInvoiceDataRequest, QueryInvoiceDigestRequest, QueryTaxpayerRequest, QueryTransactionListRequest, QueryTransactionStatusRequest, TokenExchangeRequest}

import scala.collection.JavaConverters._
import scala.io.Source
import scala.xml.Node

class suiteSerialization extends baseSuite {
  describe("The generated XSD API") {
    def parser[A](implicit format: XMLFormat[A]) =
      (nodeSequence: Node) => scalaxb.fromXML[A](nodeSequence)

    inputStreamsFromResourceDirectory("/invoices").foreach {
      case (path, inputStream) =>
        it(s"should be able to parse [$path],") {
          scalaxb.fromXML[InvoiceData] {
            scala.xml.XML.loadString(Source.fromInputStream(inputStream).mkString)
          }
        }
    }

    List(
      "manageAnnulment.xml" -> parser[ManageAnnulmentRequest],
      "manageInvoice.xml" -> parser[ManageInvoiceRequest],
      "queryInvoiceChainDigest.xml" -> parser[QueryInvoiceChainDigestRequest],
      "queryInvoiceCheck.xml" -> parser[QueryInvoiceCheckRequest],
      "queryInvoiceData.xml" -> parser[QueryInvoiceDataRequest],
      "queryInvoiceDigest_inbound_invoice_chain.xml" -> parser[QueryInvoiceDigestRequest],
      "queryInvoiceDigest_outbound_query_params.xml" -> parser[QueryInvoiceDigestRequest],
      "queryTaxpayer.xml" -> parser[QueryTaxpayerRequest],
      "queryTransactionList.xml" -> parser[QueryTransactionListRequest],
      "queryTransactionStatus.xml" -> parser[QueryTransactionStatusRequest],
      "tokenExchange.xml" -> parser[TokenExchangeRequest],
    ).foreach {
      case (testResource, parser) =>
        it(s"should be able to parse [$testResource],") {
          val manageAnnulmentXML = Source.fromResource("requests/" + testResource).mkString
          parser(scala.xml.XML.loadString(manageAnnulmentXML))
        }
    }
  }


  def inputStreamsFromResourceDirectory(directory: String): List[(String, InputStream)] = {
    val URI = getClass.getResource(directory).toURI

    log.info("Looking for XML files on URI [{}]", URI.toString)

    if (URI.getScheme == "jar") {
      log.info("URI scheme is [jar].")
      val fileSystem = FileSystems.newFileSystem(URI, Collections.emptyMap[String, Any])
      Files.walk(fileSystem.getPath(directory), 1).iterator().asScala
        .filter(_.toString.endsWith(".xml"))
        .map {
          path =>
            log.info("Opening ZIP input stream for path [{}].", path.toString)
            path.getFileName.toString -> path.toUri.toURL.openStream()
        }
        .toList
    } else {
      log.info("URI scheme is [not jar], trying to read from [local] file system.")
      Paths
        .get(URI)
        .toFile
        .ensuring(_.exists(), s"Resource [$directory] does not exist!")
        .ensuring(_.isDirectory, s"Resource [$directory] is not a directory!")
        .listFiles()
        .filter(_.getName.endsWith(".xml"))
        .map {
          path =>
            log.info("Found XML on path [{}], creating input stream.", path.toString)
            path
        }
        .map(path => path.getName -> new FileInputStream(path))
        .toList
    }
  }
}