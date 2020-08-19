package systems.enliven.invoicing.hungarian.core

class Configuration(silent: Boolean = false)(implicit
factory: Factory.forConfiguration[Configuration])
 extends configuration.Configuration[Configuration](
   "invoicing-hungarian.conf",
   "invoicing-hungarian.defaults.conf",
   true,
   Some("invoicing-hungarian"),
   silent
 )
   with Serializable

object Configuration {

  implicit object configurationFactory extends Factory.forConfiguration[Configuration] {

    override def apply(
      fromFile: String,
      fromEnvironment: Boolean,
      restrictTo: Option[String],
      silent: Boolean
    ): Configuration =
      new Configuration(silent)

  }

}
