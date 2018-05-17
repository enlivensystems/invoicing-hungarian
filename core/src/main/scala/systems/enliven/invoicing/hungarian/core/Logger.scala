package systems.enliven.invoicing.hungarian.core

import org.slf4j.LoggerFactory
import com.typesafe.scalalogging

trait Logger {
  @transient protected lazy val log: scalalogging.Logger =
    scalalogging.Logger(LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$")))
}
