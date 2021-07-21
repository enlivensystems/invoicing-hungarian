package systems.enliven.invoicing.hungarian.behaviour

import akka.actor.ActorSystem.Settings
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import com.typesafe.config.Config
import systems.enliven.invoicing.hungarian.behaviour.Connection.Protocol

class PriorityMailbox(settings: Settings, config: Config)
 extends UnboundedStablePriorityMailbox(
   PriorityGenerator {
     case _: Protocol.PriorityManageInvoice => 0
     case _: Protocol.ManageInvoiceFailed   => 1
     case _: Protocol.ManageInvoice         => 2
     case _                                 => 3
   }
 )
