package systems.enliven.invoicing.hungarian.core.configuration

import com.typesafe.config.ConfigException.{Missing, WrongType}
import com.typesafe.config._
import systems.enliven.invoicing.hungarian.core
import systems.enliven.invoicing.hungarian.core.{Factory, Logger}

import java.io.{File, Serializable}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.util.Properties
import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.language.reflectiveCalls
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
  * Abstract configuration that we enforce throughout the Hub and in our applications.
  *
  * Any specialized configuration extending this should have a default constructor and should
  * define a default value for each class parameter.
  *
  * @param fromFile Defines the file as a classpath resource from which the configuration should be
  *                 loaded. Only `CONF` files are accepted.
  * @param referencePath Path for the reference configuration from which the supplied configuration
  *                      will be validated. In addition to validation, default values will be
  *                      picked from the reference configuration as well.
  * @param fromEnvironment Whether load configuration entries from the environment.
  * @param restrictTo Restricts this configuration to the specified path. For example if `hub` is
  *                   specified, only configuration entries starting with `hub` will be accepted.
  * @param silent If false all configuration key-value pairs will be logged in the constructor.
  * @tparam Specialized Type of the specialized configuration. Its own type parameter is necessary
  *                     here to be able to construct a new instance out of nothing.
  */
abstract class Configuration[
  Specialized <: Configuration[Specialized] : ClassTag : Factory.forConfiguration](
  protected val fromFile: String,
  protected val referencePath: String,
  protected val fromEnvironment: Boolean = true,
  protected val restrictTo: Option[String] = None,
  protected val silent: Boolean = false)
 extends Logger
   with Serializable {

  protected lazy val logRedactedKeywords = List(
    "secret",
    "password",
    "key",
    "private",
    "access",
    "code",
    "token"
  )

  protected val className = implicitly[ClassTag[Specialized]].runtimeClass.getName
  val instanceNumber = Configuration.nInstances.updateInstanceCount(className)

  @transient private val _parseOptions = ConfigParseOptions.defaults()
    .setAllowMissing(false)
    .setSyntax(ConfigSyntax.CONF)

  @transient private val _resource = ConfigFactory.parseResourcesAnySyntax(fromFile, _parseOptions)

  @transient private val _defaults =
    ConfigFactory.parseResourcesAnySyntax(referencePath, _parseOptions)

  private var configuration = (if (fromEnvironment) {
                                 ConfigFactory
                                   .systemProperties()
                                   .withFallback(mergeConfigurations())
                               } else {
                                 mergeConfigurations()
                               }).withOnlyPath(restrictTo.getOrElse(""))

  configuration.checkValid(_defaults)

  log.info(
    s"This is the number [$instanceNumber] instance of configuration with class name " +
      s"[$className] restricted for keys starting with [${restrictTo.getOrElse("-")}]. " +
      s"Defaults configuration " +
      s"file [${_defaults.origin().resource()}] read from [${_defaults.origin().filename()}]. " +
      s"Overwrite configuration file [${_resource.origin().resource()}] read from " +
      s"[${_resource.origin().filename()}]. Called from:"
  )
  Thread.currentThread().getStackTrace.slice(5, 10).foreach {
    e => log.info("  " + e.toString)
  }

  if (instanceNumber > 1) {
    log.warn(
      "There should be only one configuration instance of any type in one JVM! " +
        "While it is not enforced, it is encouraged, since it may lead to unexpected " +
        "behavior in application code."
    )
  }

  if (!silent) {
    logState()
  }

  def logState(): Unit = {
    val maxLength =
      if (configuration.entrySet().asScala.nonEmpty) {
        configuration.entrySet().asScala.maxBy(_.getKey.length).getKey.length
      } else {
        0
      }
    log.info {
      configuration.entrySet().asScala.map {
        pair =>
          val lowercaseKey = pair.getKey.toLowerCase
          val maybeRedactedValue = logRedactedKeywords.find {
            keyword => lowercaseKey.contains(keyword)
          }.map(
            _ => "#redacted#"
          ).getOrElse(pair.getValue.render(ConfigRenderOptions.concise()))
          s"  ${pair.getKey}${(0 to maxLength - pair.getKey.length).map(
            _ => " "
          ).mkString("")} " +
            s"[$maybeRedactedValue]"
      }.mkString("Parameters are\n", ",\n", ".")
    }
  }

  /**
    * Reloads the configuration from the working directory.
    *
    * A file with the same name will be searched for in the application's current working directory.
    * It will validate the configuration with the pre-loaded reference.
    *
    * @note This is going to mutate the underlying configuration and return with the same reference!
    */
  def reloadFromWorking(): this.type = {
    val path = System.getProperty("user.dir") + File.separator + fromFile
    reloadFromFile(path)
  }

  /**
    * Reloads the configuration from the given file.
    *
    * It will validate the configuration with the pre-loaded reference.
    *
    * @note This is going to mutate the underlying configuration and return with the same reference!
    */
  def reloadFromFile(path: String): this.type = {
    log.info(s"Reloading configuration from path [$path].")
    setUnderlyingConfiguration {
      ConfigFactory.parseFileAnySyntax(new File(path), _parseOptions)
    }
  }

  /**
    * Sets the underlying (typesafe) configuration. It will also validate the configuration
    * with the same reference that has been loaded when this configuration has been created.
    *
    * This is package protected to avoid users mess with the underlying model of this configuration,
    * but this allows the inner factory to provide immutability and supply a new underlying
    * configuration upon `set`.
    */
  protected def setUnderlyingConfiguration(underlying: Config): this.type = {
    configuration = underlying
    this
  }

  def deleteFromWorking(): Unit = {
    val path = System.getProperty("user.dir") + File.separator + fromFile
    if (new File(path).exists()) {
      if (!new File(path).delete()) {
        throw new Configuration.Exception.File("Could not delete configuration from " +
          s"working directory [$path]!")
      }
    }
  }

  def getExternal(s: String): Iterable[(String, String)] = {
    val prefix = restrictTo.map(_ + ".").getOrElse("")
    try {
      this.underlyingConfiguration
        .withOnlyPath(s"${prefix}external.$s")
        .getConfig(s"${prefix}external")
        .entrySet()
        .asScala
        .map {
          e => (e.getKey, e.getValue.unwrapped().toString)
        }
    } catch {
      case _: com.typesafe.config.ConfigException.Missing =>
        Iterable.empty[(String, String)]
    }
  }

  def getFromDomainOption[T : TypeTag](key: String): Option[T] =
    try {
      Some(getFromDomain[T](key))
    } catch {
      case _: Missing => None
    }

  /**
    * Gets a specific `key` from the domain of this configuration.
    */
  def getFromDomain[T : TypeTag](key: String): T =
    get[T](restrictTo.map(_ + ".").getOrElse("") + key)

  /**
    * Gets all the key-values from this configuration as a string-string map.
    */
  def getAll: Map[String, String] =
    configuration.entrySet().asScala.toList.map(
      entry => (entry.getKey, entry.getValue)
    ).toMap.map {
      case (k, v) => k -> v.unwrapped().toString
    }

  /**
    * Gets the list of objects under this `key` as a map of string-string pairs.
    *
    * @throws Throwable If anything goes wrong, probably if `key` is not an object list.
    */
  def objectList(key: String): List[collection.Map[String, String]] =
    try {
      configuration.getObjectList(key).iterator().asScala.map {
        _.unwrapped().asScala.mapValues(_.toString).toMap
      }.toList
    } catch {
      case t: Throwable =>
        log.error(s"Path [$key] does not seem to be an object list. Error [${t.getClass.getName}]!")
        throw t
    }

  def getOption[T : TypeTag](key: String, logger: T => Unit = (_: T) => ()): Option[T] =
    try {
      Some(get[T](key, logger))
    } catch {
      case _: Missing =>
        log.info(s"The key [$key] is missing from the configuration.")
        None
    }

  /**
    * Gets the configuration as the type specifies.
    *
    * Unsupported types will return with `AnyRef` casted to the specified type.
    * Be aware.
    */
  def get[T : TypeTag](key: String, logger: T => Unit = (_: T) => ()): T =
    try {
      val value = (typeOf[T] match {
        case t if t =:= typeOf[String]     => configuration.getString(key)
        case t if t =:= typeOf[Duration]   => Duration(configuration.getString(key))
        case t if t =:= typeOf[Int]        => configuration.getInt(key)
        case t if t =:= typeOf[Long]       => configuration.getLong(key)
        case t if t =:= typeOf[Double]     => configuration.getDouble(key)
        case t if t =:= typeOf[Boolean]    => configuration.getBoolean(key)
        case t if t =:= typeOf[Properties] => configuration.getConfig(key).asProperties()
        case t if t =:= typeOf[List[String]] =>
          configuration.getList(key).unwrapped().asScala.toList
        case t if t =:= typeOf[Array[String]] =>
          configuration.getList(key).unwrapped().asScala.toArray
        case t if t =:= typeOf[Seq[String]] => configuration.getList(key).unwrapped().asScala
        case t if t =:= typeOf[Map[String, String]] =>
          configuration.getObject(key).unwrapped().asScala
        case t if t =:= typeOf[Map[String, List[String]]] =>
          configuration.getObject(key).unwrapped().asScala.mapValues(
            _.asInstanceOf[java.util.ArrayList[String]].asScala.toList
          ).toMap
        case t if t =:= typeOf[Map[String, Boolean]] =>
          configuration.getObject(key).unwrapped().asScala
        case _ =>
          log.warn(s"Type [${typeOf[T].getClass.getCanonicalName}] is not supported by the " +
            s"configuration.")
          configuration.getAnyRef(key).asInstanceOf[T]
      }).asInstanceOf[T]
      logger(value)
      value
    } catch {
      case t: Missing   => log.warn(s"Configuration [$key] is missing!"); throw t
      case t: WrongType => log.warn(s"Configuration [$key] is of wrong type!"); throw t
    }

  /**
    * Sets a key to the supplied value in this configuration.
    *
    * @throws core.Exception If the specified key is restricted by this configuration.
    */
  def set[T : TypeTag](key: String, value: T): Specialized = {
    if (restrictTo.nonEmpty && !key.startsWith(restrictTo.get)) {
      throw new core.Exception(
        s"The specified key [$key] is restricted by [${restrictTo.get}] in this configuration!"
      )
    }
    val newConfiguration = implicitly[Factory.forConfiguration[Specialized]]
      .apply(fromFile, fromEnvironment, restrictTo, silent)

    typeOf[T] match {
      case t if t =:= typeOf[List[String]] =>
        newConfiguration.setUnderlyingConfiguration(
          configuration.withValue(
            key,
            ConfigValueFactory.fromIterable(value.asInstanceOf[List[String]].asJava)
          )
        )
      case _ =>
        newConfiguration.setUnderlyingConfiguration(
          configuration.withValue(key, ConfigValueFactory.fromAnyRef(value))
        )
    }
  }

  def saveToWorking(): Path = {
    val path = java.lang.System.getProperty("user.dir") + File.separator + fromFile
    Files.write(
      Paths.get(path),
      underlyingConfiguration
        .root()
        .render(ConfigRenderOptions.defaults())
        .getBytes(StandardCharsets.UTF_8)
    )
  }

  def underlyingConfiguration: Config = configuration

  /**
    * Extends the default configuration with the resource configuration's values.
    */
  private def mergeConfigurations(): Config = {
    var conf = _defaults
    _resource.entrySet().asScala.foreach {
      pair => conf = conf.withValue(pair.getKey, pair.getValue)
    }
    conf
  }

  implicit private class ConfigUtils(config: Config) {

    def asProperties(): Properties = {
      val props = new Properties()

      config.entrySet().asScala.map {
        entry => entry.getKey -> entry.getValue.unwrapped().toString
      }.toMap[Object, String].foreach {
        case (k, v) => props.put(k, v)
      }
      props
    }

  }

}

object Configuration {

  protected[configuration] var nInstances = new {
    protected val map = scala.collection.mutable.Map.empty[String, Int]

    def updateInstanceCount(className: String): Int =
      synchronized {
        map.update(className, map.getOrElse(className, 0) + 1)
        map(className)
      }

  }

  object Exception {
    class Restricted(message: String) extends core.Exception(message)
    class Type(message: String) extends core.Exception(message)
    class File(message: String) extends core.Exception(message)
  }

}
