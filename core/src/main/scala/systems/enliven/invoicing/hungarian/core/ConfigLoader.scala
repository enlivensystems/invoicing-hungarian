package systems.enliven.invoicing.hungarian.core

import com.typesafe.config.{Config, ConfigValue}

import scala.collection.JavaConverters._

object ConfigLoader {

  implicit class Loader(baseConfig: Config) extends Logger {

    def load(key: String, value: ConfigValue): Config =
      if (baseConfig.hasPath(key)) {
        log.info(s"Updating entry [$key] with value [${value.toString}].")
        baseConfig.withValue(key, value)
      } else {
        log.info(s"Loaded entry [$key] with value [${value.toString}].")
        baseConfig.withValue(key, value)
      }

    def load(overrideConfig: Config): Config = {
      var conf = baseConfig
      overrideConfig.entrySet().asScala.foreach {
        entry => conf = conf.load(entry.getKey, entry.getValue)
      }
      conf
    }

  }

}
