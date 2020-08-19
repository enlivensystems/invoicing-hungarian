package systems.enliven.invoicing.hungarian.core

import com.typesafe.scalalogging
import org.slf4j.LoggerFactory

trait Logger {

  @transient protected lazy val log: scalalogging.Logger =
    scalalogging.Logger(LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$")))

}
