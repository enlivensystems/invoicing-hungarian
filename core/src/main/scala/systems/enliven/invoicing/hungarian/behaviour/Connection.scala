package systems.enliven.invoicing.hungarian.behaviour

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import akka.pattern.retry
import systems.enliven.invoicing.hungarian.api.Api.Protocol.Request.Invoices
import systems.enliven.invoicing.hungarian.api.data.NavEntity
import systems.enliven.invoicing.hungarian.api.{Api, Token}
import systems.enliven.invoicing.hungarian.behaviour.Connection.Protocol
import systems.enliven.invoicing.hungarian.core.{Configuration, Logger}
import systems.enliven.invoicing.hungarian.generated.{
  ManageInvoiceResponse,
  QueryTransactionStatusResponse,
  TokenExchangeResponse
}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

object Connection {

  def apply(configuration: Configuration): Behavior[Protocol.Command] =
    Behaviors.setup[Protocol.Message] {
      context => new Connection(configuration, context).initState
    }.narrow

  object Protocol {
    sealed trait Message
    sealed trait Command extends Message
    sealed trait PrivateCommand extends Message

    final case class QueryTransactionStatus(
      replyTo: ActorRef[Try[QueryTransactionStatusResponse]],
      transactionID: String,
      entity: NavEntity,
      returnOriginalRequest: Boolean = false)
     extends Command

    final case class ManageInvoice(
      replyTo: ActorRef[Try[ManageInvoiceResponse]],
      invoices: Invoices,
      entity: NavEntity)
     extends Command

    final case class PriorityManageInvoice(
      token: Token,
      manageInvoice: ManageInvoice)
     extends PrivateCommand

    final case class TokenLoadFailed(throwable: Throwable) extends PrivateCommand
  }

}

class Connection private (
  configuration: Configuration,
  context: ActorContext[Protocol.Message])
 extends Logger {

  private val maxRetry: Int = configuration.get[Int]("invoicing-hungarian.connection.maxRetry")

  implicit private val executionContext: ExecutionContextExecutor =
    context.system.dispatchers.lookup(
      DispatcherSelector.fromConfig("akka.actor.blocking-dispatcher")
    )

  implicit private val scheduler: Scheduler =
    context.system.classicSystem.scheduler

  private val api =
    new Api()(configuration, context.system.classicSystem, context.executionContext)

  private def initState: Behavior[Protocol.Message] =
    Behaviors.receiveMessage {
      case Protocol.QueryTransactionStatus(replyTo, transactionID, entity, returnOriginalRequest) =>
        log.trace(
          "Received [query-transaction-status] request with transaction ID [{}].",
          transactionID
        )
        queryTransactionStatus(replyTo, transactionID, entity, returnOriginalRequest)
        Behaviors.same
      case Protocol.ManageInvoice(replyTo, invoices, entity) =>
        log.trace("Received [manage-invoice] request.")

        context.pipeToSelf(refreshToken(entity)) {
          case Success(response: TokenExchangeResponse) =>
            Protocol.PriorityManageInvoice(
              new Token(response, entity.credentials.exchangeKey),
              Protocol.ManageInvoice(replyTo, invoices, entity)
            )
          case Failure(throwable: Throwable) => Protocol.TokenLoadFailed(throwable)
        }

        Behaviors.same
      case Protocol.PriorityManageInvoice(
            token,
            Protocol.ManageInvoice(replyTo, entity, invoices)
          ) =>
        log.trace("Running [manage-invoice] with token [{}].", token.value)

        manageInvoice(invoices, entity)(token).onComplete {
          case Success(response: Try[ManageInvoiceResponse]) =>
            log.trace("Finish Invoice.")
            replyTo ! response
          case Failure(throwable: Throwable) =>
            log.trace("Finish Invoice.")
            replyTo ! Failure(throwable)
        }
        Behaviors.same
      case Protocol.TokenLoadFailed(_) =>
        Behaviors.same
      case _ =>
        Behaviors.unhandled
    }

  private def queryTransactionStatus(
    replyTo: ActorRef[Try[QueryTransactionStatusResponse]],
    transactionID: String,
    entity: NavEntity,
    returnOriginalRequest: Boolean
  ): Unit =
    api.queryTransactionStatus(transactionID, entity, returnOriginalRequest).onComplete {
      case Success(value) =>
        log.debug(
          "Finished with [query-transaction-status] for transaction ID [{}].",
          transactionID
        )
        replyTo ! value
      case Failure(exception) =>
        log.error(
          "Could not manage invoice due to [{}] with message [{}]!",
          exception.getClass.getName,
          exception.getMessage
        )
    }

  private def manageInvoice(entity: NavEntity, invoices: Invoices)(
    implicit token: Token
  ): Future[Try[ManageInvoiceResponse]] =
    api.manageInvoice(entity, invoices.toRequest)
      .flatMap {
        result: Try[ManageInvoiceResponse] =>
          log.trace("Request [manage-invoice] finished.")
          Future.successful[Try[ManageInvoiceResponse]](result)
      }
      .recoverWith {
        case throwable: Throwable =>
          log.error(
            "Could not manage invoice due to [{}] with message [{}]4",
            throwable.getClass.getName,
            throwable.getMessage
          )
          Future.failed[Try[ManageInvoiceResponse]](throwable)
      }

  private def refreshToken(entity: NavEntity): Future[TokenExchangeResponse] =
    retry(
      () => api.tokenExchange(entity),
      maxRetry,
      attempted => Option(200.milliseconds * attempted)
    ).flatMap {
      case Success(response) => Future.successful[TokenExchangeResponse](response)
      case Failure(throwable: Throwable) =>
        log.error(
          "Could not refresh exchange token due to [{}] with message [{}]!",
          throwable.getClass.getSimpleName,
          throwable.getMessage
        )
        Future.failed[TokenExchangeResponse](throwable)
    }.recoverWith {
      case throwable: Throwable =>
        log.error(
          "Could not refresh exchange token due to [{}] with message [{}]!",
          throwable.getClass.getSimpleName,
          throwable.getMessage
        )
        Future.failed[TokenExchangeResponse](throwable)
    }

}
