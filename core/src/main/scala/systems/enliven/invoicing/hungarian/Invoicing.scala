package systems.enliven.invoicing.hungarian

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import systems.enliven.invoicing.hungarian.api.Api.Protocol.Request.Invoices
import systems.enliven.invoicing.hungarian.behaviour.{Connection, Guardian}
import systems.enliven.invoicing.hungarian.core.{Configuration, Logger}
import systems.enliven.invoicing.hungarian.generated.{
  ManageInvoiceResponse,
  QueryTransactionStatusResponse
}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

class Invoicing()(implicit configuration: Configuration) extends Logger {

  implicit val typedSystem: ActorSystem[Guardian.Protocol.Command] =
    ActorSystem.create[Guardian.Protocol.Command](
      Guardian.apply(),
      "guardian",
      ConfigFactory.load("actor.conf")
    )

  private var connection: Option[ActorRef[Connection.Protocol.Command]] = None

  def classicSystem: akka.actor.ActorSystem = typedSystem.classicSystem

  def init(): Unit = {
    connect().onComplete {
      case Success(connectionPool) =>
        connection = Some(connectionPool.pool)
      case Failure(exception) =>
        log.error("Could not refresh exchange token due to [{}]", exception.getMessage)
    }(executionContext)
  }

  private def connect(): Future[Guardian.Protocol.ConnectionPool] =
    retry.Backoff(10, 1.seconds).apply {
      implicit val askTimeout: Timeout = 30.seconds
      typedSystem.ask[Guardian.Protocol.ConnectionPool](
        replyTo => Guardian.Protocol.GetConnectionPool(replyTo)
      )
    }(retry.Success.always, executionContext)

  def executionContext: ExecutionContextExecutor = typedSystem.executionContext

  def isReady: Boolean = connection.isDefined

  def awaitShutdown(): Unit = {
    log.debug("Shutting down actor system...")
    typedSystem ! Guardian.Protocol.Shutdown
    Await.result(typedSystem.whenTerminated, 10.seconds)
    log.info("Terminated actor system.")
  }

  def awaitInit(): Unit = {
    connection = Some(Await.result(connect(), Duration.Inf).pool)
  }

  def invoices(invoices: Invoices, timeout: FiniteDuration)(implicit
  askTimeout: Timeout): Try[ManageInvoiceResponse] =
    Await.result(
      connection.get.ask[Try[ManageInvoiceResponse]](
        replyTo => Connection.Protocol.ManageInvoice(replyTo, invoices)
      ),
      timeout
    )

  def invoices(invoices: Invoices)(
    implicit askTimeout: Timeout,
    maxRetry: Int = 10,
    baseDelay: FiniteDuration = 100.milliseconds
  ): Future[Try[ManageInvoiceResponse]] =
    retry.Backoff(maxRetry, baseDelay).apply {
      connection.get.ask[Try[ManageInvoiceResponse]](
        replyTo => Connection.Protocol.ManageInvoice(replyTo, invoices)
      )
    }(retry.Success.always, typedSystem.executionContext)

  def status(
    transactionID: String,
    timeout: FiniteDuration
  )(implicit askTimeout: Timeout): Try[QueryTransactionStatusResponse] =
    status(transactionID, returnOriginalRequest = false, timeout)(askTimeout)

  def status(
    transactionID: String,
    returnOriginalRequest: Boolean = false,
    timeout: FiniteDuration
  )(implicit askTimeout: Timeout): Try[QueryTransactionStatusResponse] =
    Await.result(
      connection.get.ask[Try[QueryTransactionStatusResponse]](
        replyTo =>
          Connection.Protocol.QueryTransactionStatus(replyTo, transactionID, returnOriginalRequest)
      ),
      timeout
    )

  def status(transactionID: String)(implicit
  askTimeout: Timeout): Future[Try[QueryTransactionStatusResponse]] =
    status(transactionID, returnOriginalRequest = false)(askTimeout)

  def status(transactionID: String, returnOriginalRequest: Boolean)(implicit
  askTimeout: Timeout): Future[Try[QueryTransactionStatusResponse]] =
    connection.get.ask[Try[QueryTransactionStatusResponse]](
      replyTo =>
        Connection.Protocol.QueryTransactionStatus(
          replyTo,
          transactionID,
          returnOriginalRequest
        )
    )

}
