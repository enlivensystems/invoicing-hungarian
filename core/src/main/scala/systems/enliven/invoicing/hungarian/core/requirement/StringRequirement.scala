package systems.enliven.invoicing.hungarian.core.requirement

case class StringRequirement(fieldName: String, valueOption: Option[String]) extends Requirement {

  private def checkIfExist(f: String => Unit): StringRequirement = {
    valueOption.foreach(f(_))
    this
  }

  def limit(limit: Int): StringRequirement =
    checkIfExist {
      value =>
        requirement(value.length <= limit)(
          Requirement.Exception.CharacterLimitViolation(
            s"Field [$fieldName] violated character limit [$limit] (${value.length})."
          )
        )
    }

  def nonEmpty: StringRequirement = minLimit(1)

  def minLimit(minLimit: Int): StringRequirement =
    checkIfExist {
      value =>
        requirement(minLimit <= value.length)(
          Requirement.Exception.CharacterLimitViolation(
            s"Field [$fieldName] violated character minimum limit [$minLimit] (${value.length})."
          )
        )
    }

  def trimmed: StringRequirement =
    checkIfExist {
      value =>
        requirement(value.trim.length == value.length)(
          Requirement.Exception.TrimViolation(
            s"Field [$fieldName] violated trim requirement ($value)."
          )
        )
    }

  def matchesAnyOf(regexes: String*): StringRequirement =
    checkIfExist {
      value =>
        requirement(regexes.exists(regex => value.matches(regex)))(
          Requirement.Exception.RegexViolation(
            s"Field [$fieldName] violated regex requirement ${regexes.mkString("[", "], [", "]")} ($value)."
          )
        )
    }

  def matches(regex: String): StringRequirement =
    checkIfExist {
      value =>
        requirement(value.matches(regex))(
          Requirement.Exception.RegexViolation(
            s"Field [$fieldName] violated regex requirement [$regex] ($value)."
          )
        )
    }

}

object StringRequirement {

  implicit class advancedStringRequirements(value: String) {
    def named(fieldName: String): StringRequirement = StringRequirement(fieldName, value)
  }

  implicit class advancedStringOptionRequirements(value: Option[String]) {
    def named(fieldName: String): StringRequirement = StringRequirement(fieldName, value)
  }

  def apply(fieldName: String, value: String): StringRequirement =
    StringRequirement(fieldName, Some(value))

}
