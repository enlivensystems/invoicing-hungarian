package systems.enliven.invoicing.hungarian.behaviour

import akka.actor.typed.scaladsl.{Behaviors, PoolRouter, Routers}
import akka.actor.typed.{MailboxSelector, SupervisorStrategy}
import systems.enliven.invoicing.hungarian.core.Configuration

object ConnectionPool {

  def apply(configuration: Configuration): PoolRouter[Connection.Protocol.Command] = {
    val poolSize: Int = configuration.get[Int]("invoicing-hungarian.connection.pool")

    Routers.pool(poolSize) {
      Behaviors
        .supervise(Connection.apply(configuration))
        .onFailure[scala.Exception](SupervisorStrategy.restart)
    }.withRouteeProps(MailboxSelector.fromConfig("akka.actor.invoicing-priority-dispatcher"))
  }

}
