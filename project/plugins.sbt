logLevel := Level.Warn

addDependencyTreePlugin
addSbtPlugin("com.github.cb372" % "sbt-explicit-dependencies" % "0.3.1")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.9")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.3.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.11.1")

resolvers ++= Resolver.sonatypeOssRepos("public")
addSbtPlugin("org.scalaxb" % "sbt-scalaxb" % "1.12.0")
