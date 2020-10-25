import Dependencies._
import sbt.Keys.test
import sbt.Tests.{Group, SubProcess}
import sbtrelease.ReleaseStateTransformations.{
  commitNextVersion,
  commitReleaseVersion,
  inquireVersions,
  pushChanges,
  setNextVersion,
  setReleaseVersion
}
import sbtrelease.{versionFormatError, Version}

lazy val commonSettings = Seq(
  organizationName := "Enliven Systems Kft.",
  organization := "systems.enliven.invoicing.hungarian",
  scalaVersion := "2.12.12",
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  addCompilerPlugin(scalafixSemanticdb),
  scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.3",
  scalacOptions ++= List(
    "-Yrangepos",
    "-Ywarn-unused-import",
    "-encoding",
    "UTF-8",
    "-target:jvm-1.8",
    "-deprecation",
    "-feature",
    "-unchecked",
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
  concurrentRestrictions in Global := Seq(Tags.limitAll(14)),
  parallelExecution in Test := true,
  testGrouping in Test := (testGrouping in Test).value.flatMap {
    group =>
      group.tests.map(
        test => Group(test.name, Seq(test), SubProcess(ForkOptions()))
      )
  },
  concurrentRestrictions := Seq(Tags.limit(Tags.ForkedTestGroup, 14)),
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
  logLevel in test := Level.Debug,
  /** Do not pack sources in compile tasks.
    */
  sources in (Compile, doc) := Seq.empty,
  /** Disabling Scala and Java documentation in publishing tasks.
    */
  publishArtifact in (Compile, packageDoc) := false,
  publishArtifact in (Test, packageDoc) := false,
  publishArtifact in (Test, packageBin) := true,
  publishArtifact in (Test, packageSrc) := true,
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
  publishTo in ThisBuild := Some(
    "Artifactory Realm".at(s"https://central.enliven.systems/artifactory/sbt-release/")
  ),
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials.central"),
  resolvers ++= Seq("Maven Central".at("https://repo1.maven.org/maven2/"))
)

lazy val core =
  (project in file("core")).settings(commonSettings: _*).enablePlugins(ScalaxbPlugin).settings(
    name := "core",
    description := "Core Hungarian Invoicing API to interface with NAV Online Invoice API 2.0.",
    libraryDependencies ++= coreDependencies,
    scalaxbDispatchVersion in (Compile, scalaxb) := "0.13.4",
    scalaxbPackageName in (Compile, scalaxb) := "systems.enliven.invoicing.hungarian.generated"
  )
