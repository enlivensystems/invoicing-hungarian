package systems.enliven.invoicing.hungarian.core.requirement

import systems.enliven.invoicing.hungarian.core

trait Requirement {

  def requirement[T <: Throwable](condition: Boolean)(throwable: T): Unit = {
    if (!condition)
      throw throwable
  }

}

object Requirement {
  abstract class Exception(message: String) extends core.Exception(message)

  object Exception {

    case class CharacterLimitViolation(message: String) extends Exception(message: String)
    case class TrimViolation(message: String) extends Exception(message: String)
    case class RegexViolation(message: String) extends Exception(message: String)
  }

}
