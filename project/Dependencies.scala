import sbt._

object Dependencies {

  val coreDependencies = Seq(
    "org.scalatest" %% "scalatest" % "3.2.16" % "test",
    "org.scala-lang.modules" %% "scala-xml" % "2.1.0",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.2.0",
    "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
    "org.http4s" %% "http4s-ember-client" % "0.23.19",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
    "org.slf4j" % "slf4j-api" % "2.0.7",
    "org.apache.logging.log4j" % "log4j-slf4j2-impl" % "2.20.0",
    "com.typesafe.akka" %% "akka-actor" % "2.8.0",
    "com.typesafe.akka" %% "akka-stream" % "2.8.0",
    "com.typesafe.akka" %% "akka-slf4j" % "2.8.0",
    "com.typesafe.akka" %% "akka-actor-typed" % "2.8.0",
    "com.typesafe.akka" %% "akka-http" % "10.5.0",
    "org.apache.commons" % "commons-lang3" % "3.12.0",
    "javax.xml.bind" % "jaxb-api" % "2.3.1",
    "com.softwaremill.retry" %% "retry" % "0.3.6",
    "org.bouncycastle" % "bcprov-jdk18on" % "1.73",
    "joda-time" % "joda-time" % "2.12.5",
    "net.datafaker" % "datafaker" % "2.0.0",
    "com.github.mifmif" % "generex" % "1.0.2"
  )

}
