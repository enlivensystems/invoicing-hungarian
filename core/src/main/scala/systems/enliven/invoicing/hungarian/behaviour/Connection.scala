package systems.enliven.invoicing.hungarian.behaviour

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import akka.pattern.retry
import systems.enliven.invoicing.hungarian.api.Api.Protocol.Request.Invoices
import systems.enliven.invoicing.hungarian.api.data.NavEntity
import systems.enliven.invoicing.hungarian.api.{Api, Token}
import systems.enliven.invoicing.hungarian.behaviour.Connection.Protocol
import systems.enliven.invoicing.hungarian.core
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

  protected val invalidRequestSignature: String = """ERROR \[INVALID_REQUEST_SIGNATURE].*"""
  protected val invalidSecurityUser: String = """ERROR \[INVALID_SECURITY_USER].*"""
  protected val notRegisteredCustomer: String = """ERROR \[NOT_REGISTERED_CUSTOMER].*"""

  def apply(configuration: Configuration): Behavior[Protocol.Command] =
    Behaviors.setup[Protocol.Message] {
      context => new Connection(configuration, context).initState
    }.narrow

  object Protocol {
    sealed trait Message
    sealed trait Command extends Message
    sealed trait PrivateCommand extends Message

    final case class ValidateEntity(
      replyTo: ActorRef[Try[Unit]],
      entity: NavEntity
    )
     extends Command

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
      DispatcherSelector.fromConfig("akka.actor.invoicing-blocking-dispatcher")
    )

  implicit private val scheduler: Scheduler =
    context.system.classicSystem.scheduler

  private val api =
    new Api()(configuration, context.system.classicSystem, context.executionContext)

  private def initState: Behavior[Protocol.Message] =
    Behaviors.receiveMessage {
      case Protocol.ValidateEntity(replyTo, entity) =>
        log.trace("Received [validate-entity] request.")

        refreshToken(entity).map(
          response => new Token(response, entity.credentials.exchangeKey)
        ).onComplete {
          case Success(_) =>
            log.trace("Successfully validated entity!")
            replyTo ! scala.util.Success((): Unit)
          case Failure(throwable) =>
            log.trace("Entity validation failed!")
            val message = throwable.getMessage
            replyTo ! scala.util.Failure(
              throwable match {
                case _: java.security.InvalidKeyException =>
                  core.Exception.InvalidExchangeKey(message)
                case _: javax.crypto.BadPaddingException =>
                  core.Exception.InvalidExchangeKey(message)
                case _: core.Exception if message.matches(Connection.invalidRequestSignature) =>
                  core.Exception.InvalidRequestSignature(message)
                case _: core.Exception if message.matches(Connection.invalidSecurityUser) =>
                  core.Exception.InvalidSecurityUser(message)
                case _: core.Exception if message.matches(Connection.notRegisteredCustomer) =>
                  core.Exception.NotRegisteredCustomer(message)
                case _ =>
                  throwable
              }
            )
        }

        Behaviors.same
      case Protocol.QueryTransactionStatus(replyTo, transactionID, entity, returnOriginalRequest) =>
        log.trace(
          "Received [query-transaction-status] request with transaction ID [{}].",
          transactionID
        )

        api.queryTransactionStatus(transactionID, entity, returnOriginalRequest).onComplete {
          case Success(value) =>
            log.debug(
              "Finished [query-transaction-status] request with transaction ID [{}].",
              transactionID
            )
            replyTo ! value
          case Failure(exception) =>
            log.error(
              "Failed [query-transaction-status] request with transaction ID [{}] " +
                "due to [{}] with message [{}]!",
              transactionID,
              exception.getClass.getName,
              exception.getMessage
            )
        }

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
    ).flatMap(Future.fromTry).recoverWith {
      case throwable: Throwable =>
        log.error(
          "Could not refresh exchange token due to [{}] with message [{}]!",
          throwable.getClass.getSimpleName,
          throwable.getMessage
        )
        Future.failed[TokenExchangeResponse](throwable)
    }

}
