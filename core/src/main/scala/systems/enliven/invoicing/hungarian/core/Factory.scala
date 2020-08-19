package systems.enliven.invoicing.hungarian.core

abstract class Factory[E] {
  def apply(): E
}

object Factory {

  abstract class forConfiguration[T] extends Serializable {

    def apply(
      fromFile: String,
      fromEnvironment: Boolean = true,
      restrictTo: Option[String] = None,
      silent: Boolean = false
    ): T

  }

}
