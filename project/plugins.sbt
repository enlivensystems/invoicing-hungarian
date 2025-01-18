logLevel := Level.Warn

addDependencyTreePlugin
addSbtPlugin("com.github.cb372" % "sbt-explicit-dependencies" % "0.3.1")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.3.0")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.4.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.0")

resolvers ++= Resolver.sonatypeOssRepos("public")
addSbtPlugin("org.scalaxb" % "sbt-scalaxb" % "1.12.1")
