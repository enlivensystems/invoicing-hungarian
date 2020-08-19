package systems.enliven.invoicing.hungarian.behaviour

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import systems.enliven.invoicing.hungarian.behaviour.Guardian.Protocol.GetConnectionPool
import systems.enliven.invoicing.hungarian.core.{Configuration, Logger}

import scala.concurrent.duration._

object Guardian extends Logger {

  def apply()(implicit configuration: Configuration): Behavior[Protocol.Command] =
    Behaviors.setup[Protocol.Message] {
      context =>
        val connectionPool: ActorRef[Connection.Protocol.Command] =
          context.spawn(createConnectionPool(configuration), "connection-pool")

        Behaviors.withTimers {
          timers =>
            Behaviors.receiveMessage {
              case GetConnectionPool(replyTo) =>
                replyTo ! Protocol.ConnectionPool(connectionPool)
                Behaviors.same
              case Protocol.Shutdown =>
                context.watchWith(connectionPool, Protocol.PoolShutdown)
                log.debug("Connection pool is shutting down!")
                context.stop(connectionPool)
                timers.startSingleTimer(TimerKey, Protocol.ForceShutdown, 5.seconds)
                Behaviors.same
              case Protocol.PoolShutdown =>
                Behaviors.stopped(() => log.debug("Guardian is shutting down gracefully!"))
              case Protocol.ForceShutdown =>
                Behaviors.stopped(() => log.debug("Guardian is shutting down forcefully!"))
            }
        }
    }.narrow

  private def createConnectionPool(
    configuration: Configuration
  ): Behavior[Connection.Protocol.Command] =
    Behaviors
      .supervise(ConnectionPool.apply(configuration))
      .onFailure[scala.Exception](SupervisorStrategy.restart)

  object Protocol {
    sealed trait Message
    sealed trait Command extends Message
    sealed trait PrivateCommand extends Message

    final case class GetConnectionPool(replyTo: ActorRef[ConnectionPool]) extends Command
    final case class ConnectionPool(pool: ActorRef[Connection.Protocol.Command])

    final case object Shutdown extends Command
    final case object PoolShutdown extends PrivateCommand
    final case object ForceShutdown extends PrivateCommand
  }

  final private case object TimerKey

}
