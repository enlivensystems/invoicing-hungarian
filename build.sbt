import Dependencies.*
import sbtrelease.ReleaseStateTransformations.*
import sbtrelease.{versionFormatError, Version}
import sbtrelease.ReleasePlugin.autoImport.{releaseProcess, releaseVersionBump}
import sbt.Tests.{Group, SubProcess}
import sbt.{Credentials, Test}

lazy val commonSettings = Seq(
  organizationName := "Enliven Systems Kft.",
  organization := "systems.enliven.invoicing.hungarian",
  scalaVersion := "2.13.12",
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  addCompilerPlugin(scalafixSemanticdb),
  scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0",
  ThisBuild / scalafixScalaBinaryVersion := "2.13",
  scalacOptions ++= List(
    "-Yrangepos",
    "-encoding",
    "UTF-8",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Ywarn-unused",
    "-language:implicitConversions",
    "-language:postfixOps"
  ),
  releaseVersionBump := sbtrelease.Version.Bump.Next,
  releaseIgnoreUntrackedFiles := true,
  releaseVersion := {
    ver =>
      Version(ver).map(_.withoutQualifier.string)
        .getOrElse(versionFormatError(ver))
  },
  releaseNextVersion := {
    ver =>
      Version(ver).map(_.bump(releaseVersionBump.value).withoutQualifier.string)
        .getOrElse(versionFormatError(ver))
  },
  releaseProcess := Seq[ReleaseStep](
    inquireVersions,
    setReleaseVersion,
    commitReleaseVersion,
    setNextVersion,
    commitNextVersion,
    pushChanges
  ),
  Global / concurrentRestrictions := Seq(Tags.limitAll(14)),
  pomExtra :=
    <developers>
      <developer>
        <id>horvath-martin</id>
        <name>Martin Horváth</name>
      </developer>
      <developer>
        <id>zzvara</id>
        <name>Zoltán Zvara</name>
      </developer>
    </developers>,
  Test / fork := true,
  Test / testForkedParallel := true,
  Test / parallelExecution := true,
  Test / testGrouping := (Test / testGrouping).value.flatMap {
    group =>
      group.tests.map(test =>
        Group(
          test.name,
          Seq(test),
          SubProcess(ForkOptions().withRunJVMOptions(scala.Vector(
            "-Dlog4j.configurationFile=scala-build-tool-resources/log4j2.properties"
          )))
        )
      )
  },
  concurrentRestrictions := Seq(Tags.limit(Tags.ForkedTestGroup, 3)),
  Test / logLevel := Level.Info,
  /**
    * Do not pack sources in compile tasks.
    */
  Compile / doc / sources := Seq.empty,
  /**
    * Disabling Scala and Java documentation in publishing tasks.
    */
  Compile / packageDoc / publishArtifact := false,
  Test / packageDoc / publishArtifact := false,
  Test / packageBin / publishArtifact := true,
  Test / packageSrc / publishArtifact := true,
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
  publishTo := Some(
    "Artifactory Realm".at(s"https://central.enliven.systems/artifactory/sbt-release/")
  ),
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials.central"),
  resolvers ++= Seq("Maven Central".at("https://repo1.maven.org/maven2/"))
)

lazy val core =
  (project in file("core"))
    .settings(commonSettings: _*)
    .enablePlugins(ScalaxbPlugin)
    .settings(
      name := "core",
      description := "Core Hungarian Invoicing API to interface with NAV Online Invoice API 3.0.",
      libraryDependencies ++= coreDependencies,
      scalaxbGenerateDispatchClient := false,
      scalaxbGenerateHttp4sClient := true,
      scalaxbPackageName := "systems.enliven.invoicing.hungarian.generated"
    )

lazy val invoicing = (project in file("."))
  .settings(commonSettings: _*)
  .enablePlugins(ScalaxbPlugin)
  .aggregate(core)
  .dependsOn(core % "test->test;compile->compile")
