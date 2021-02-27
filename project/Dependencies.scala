import sbt._

object Dependencies {

  val coreDependencies = Seq(
    "org.scala-lang" % "scala-library" % "2.13.5",
    "org.scala-lang" % "scala-compiler" % "2.13.5",
    "org.scalatest" %% "scalatest" % "3.3.0-SNAP3" % "test",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "org.slf4j" % "slf4j-api" % "1.7.30",
    "org.slf4j" % "slf4j-log4j12" % "1.7.30",
    "com.typesafe.akka" %% "akka-actor" % "2.6.13",
    "com.typesafe.akka" %% "akka-stream" % "2.6.13",
    "com.typesafe.akka" %% "akka-http" % "10.2.4",
    "com.typesafe.akka" %% "akka-slf4j" % "2.6.13",
    "com.typesafe.akka" %% "akka-actor-typed" % "2.6.13",
    "org.apache.commons" % "commons-lang3" % "3.11",
    "org.scala-lang.modules" %% "scala-xml" % "1.3.0",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
    "net.databinder.dispatch" %% "dispatch-core" % "0.13.4",
    "javax.xml.bind" % "jaxb-api" % "2.3.1",
    "com.softwaremill.retry" %% "retry" % "0.3.3",
    "org.bouncycastle" % "bcprov-jdk15on" % "1.68",
    "com.github.javafaker" % "javafaker" % "1.0.2" % "test"
  )

}
