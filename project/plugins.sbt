logLevel := Level.Warn

addDependencyTreePlugin
addSbtPlugin("com.github.cb372" % "sbt-explicit-dependencies" % "0.3.1")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.7")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.4")

resolvers ++= Resolver.sonatypeOssRepos("public")
addSbtPlugin("org.scalaxb" % "sbt-scalaxb" % "1.11.0")