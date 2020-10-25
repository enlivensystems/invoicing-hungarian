package systems.enliven.invoicing.hungarian.behaviour

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import akka.pattern.retry
import systems.enliven.invoicing.hungarian.api.Api.Protocol.Request.Invoices
import systems.enliven.invoicing.hungarian.api.{Api, Token}
import systems.enliven.invoicing.hungarian.behaviour.Connection.Protocol
import systems.enliven.invoicing.hungarian.core.{Configuration, Logger}
import systems.enliven.invoicing.hungarian.generated.{
  ManageInvoiceResponse,
  QueryTransactionStatusResponse,
  TokenExchangeResponse
}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object Connection {

  def apply(configuration: Configuration): Behavior[Protocol.Command] = {
    val stashCapacity: Int = configuration.get[Int]("invoicing-hungarian.connection.stash")

    Behaviors.setup[Protocol.Message] {
      context =>
        Behaviors.withStash(stashCapacity) {
          buffer =>
            Behaviors.withTimers {
              timers => new Connection(configuration, timers, buffer, context).initState
            }
        }
    }.narrow
  }

  object Protocol {
    sealed trait Message
    sealed trait Command extends Message
    sealed trait PrivateCommand extends Message

    final case class QueryTransactionStatus(
      replyTo: ActorRef[Try[QueryTransactionStatusResponse]],
      transactionID: String,
      returnOriginalRequest: Boolean = false)
     extends Command

    final case class ManageInvoice(
      replyTo: ActorRef[Try[ManageInvoiceResponse]],
      invoices: Invoices)
     extends Command

    final case class WrappedManageInvoice(
      replyTo: ActorRef[Try[ManageInvoiceResponse]],
      invoices: Invoices)
     extends PrivateCommand

    final case object PreloadToken extends PrivateCommand
  }

  final private case object TimerKey

}

class Connection private (
  configuration: Configuration,
  timers: TimerScheduler[Protocol.Message],
  buffer: StashBuffer[Protocol.Message],
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

  /**
    * According to the API documentation "single-use data reporting token".
    */
  private var tokens: Seq[Token] = Seq.empty
  private var preloadedToken: Option[Token] = None

  private def initState: Behavior[Protocol.Message] = {
    timers.startSingleTimer(Connection.TimerKey, Protocol.PreloadToken, 100.milliseconds)
    Behaviors.receiveMessage {
      case Protocol.QueryTransactionStatus(replyTo, transactionID, returnOriginalRequest) =>
        log.debug(
          "Received [query-transaction-status] request with transaction ID [{}].",
          transactionID
        )
        queryTransactionStatus(replyTo, transactionID, returnOriginalRequest)

        Behaviors.same
      case Protocol.ManageInvoice(replyTo, invoices) =>
        log.debug("Received [manage-invoice] request.")
        if (preloadedToken.isDefined) {
          log.info("Using preloaded token [{}].", preloadedToken.get)
          implicit val token: Token = preloadedToken.get
          preloadedToken = None
          timers.startSingleTimer(Connection.TimerKey, Protocol.PreloadToken, 1.seconds)
          manageInvoice(replyTo, invoices)
        } else {
          log.debug("Requesting new token.")
          buffer.stash(Protocol.WrappedManageInvoice(replyTo, invoices))
          refreshToken {
            response: TokenExchangeResponse =>
              val token = new Token(response, api.getExchangeKey)
              tokens = tokens :+ token
              log.debug("New token [{}] acquired.", token.value)
              buffer.unstash(Behaviors.same, 1, identity)
          }
        }
        Behaviors.same
      case Protocol.WrappedManageInvoice(replyTo, invoices) =>
        implicit val token: Token = tokens.head
        tokens = tokens.tail
        log.debug("Running [manage-invoice] with token [{}].", token.value)
        manageInvoice(replyTo, invoices)
        Behaviors.same
      case Protocol.PreloadToken =>
        log.debug("Preloading token.")
        refreshToken {
          response: TokenExchangeResponse =>
            preloadedToken = Some(new Token(response, api.getExchangeKey))
            log.debug("Token [{}] preloaded.", preloadedToken.get.value)
            timers.startSingleTimer(
              Connection.TimerKey,
              Protocol.PreloadToken,
              4.minutes + 30.seconds
            )
        }
        Behaviors.same
      case _ =>
        Behaviors.unhandled
    }
  }

  private def queryTransactionStatus(
    replyTo: ActorRef[Try[QueryTransactionStatusResponse]],
    transactionID: String,
    returnOriginalRequest: Boolean
  ): Unit =
    api.queryTransactionStatus(transactionID, returnOriginalRequest).onComplete {
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

  private def manageInvoice(
    replyTo: ActorRef[Try[ManageInvoiceResponse]],
    invoices: Invoices
  )(implicit token: Token): Unit =
    api.manageInvoice(invoices.toRequest).onComplete {
      case Success(value) =>
        log.debug("Request [manage-invoice] finished.")
        replyTo ! value
      case Failure(exception) =>
        log.error(
          "Could not manage invoice due to [{}] with message [{}]4",
          exception.getClass.getName,
          exception.getMessage
        )
    }

  private def refreshToken(onSuccess: TokenExchangeResponse => Unit): Unit = {
    retry(
      () => api.tokenExchange(),
      maxRetry,
      attempted => Option(200.milliseconds * attempted)
    ).onComplete {
      case Success(result) =>
        result match {
          case Success(response) =>
            onSuccess(response)
          case Failure(exception) =>
            log.error("Could not refresh exchange token due to [{}]!", exception.getMessage)
        }
      case Failure(throwable: Throwable) => // Network error
        log.error(
          "Could not refresh exchange token due to [{}] with message [{}]!",
          throwable.getClass.getSimpleName,
          throwable.getMessage
        )
    }
  }

}
