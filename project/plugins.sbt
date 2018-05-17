logLevel := Level.Warn

resolvers += Resolver.sonatypeRepo("public")

addSbtPlugin("com.github.cb372" % "sbt-explicit-dependencies" % "0.2.13")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")
addSbtPlugin("org.scalaxb" % "sbt-scalaxb" % "1.7.5")