package systems.enliven.invoicing.hungarian.api

import scala.xml.{Elem, XML}

/**
 * The API in practice does not follow the documentation and in some cases
 * mandatory parts of the response are missing. (e.g. indexing from 0 in manage invoice)
 * [[Fixer.fixResponse]] injects the missing parts with mock data to avoid parsing errors.
 */
object Fixer {
  private lazy val header: Elem = {
    <header>
      <requestId>mBUzbLnCBpAQTHghB5TV3tOXETRc8g</requestId>
      <timestamp>2020-07-22T09:40:10.972Z</timestamp>
      <requestVersion>2.0</requestVersion>
    </header>
  }

  private lazy val software: Elem = {
    <software>
      <softwareId>ABCDEFGHI123456789</softwareId>
      <softwareName>name</softwareName>
      <softwareOperation>ONLINE_SERVICE</softwareOperation>
      <softwareMainVersion>0.0.0</softwareMainVersion>
      <softwareDevName>name</softwareDevName>
      <softwareDevContact>contact</softwareDevContact>
    </software>
  }

  private lazy val addHeader: Elem => Elem = {
    case root if (root \ "header").isEmpty =>
      root.copy(child = header +: root.child)
    case root => root
  }

  private lazy val addSoftware: Elem => Elem = {
    case root if (root \ "software").isEmpty =>
      val splitPoint: Int = root.child.indexWhere(_.label == "technicalValidationMessages")
      val (first, second) = root.child.splitAt(splitPoint)
      root.copy(child = first ++ software ++ second)
    case root => root
  }

  private lazy val transform: Elem => Elem = addHeader andThen addSoftware

  def fixResponse(xml: String): String = {
    val root: Elem = XML.loadString(xml)
    transform(root).toString()
  }

}
