logLevel := Level.Warn

resolvers += Resolver.sonatypeRepo("public")

addSbtPlugin("com.github.cb372" % "sbt-explicit-dependencies" % "0.2.16")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")
addSbtPlugin("org.scalaxb" % "sbt-scalaxb" % "1.8.3")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.3")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.1")
