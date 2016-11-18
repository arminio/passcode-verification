import sbt.Keys._
import sbt._

object HmrcBuild extends Build {

  import uk.gov.hmrc._
  import uk.gov.hmrc.versioning.SbtGitVersioning

  val appName = "passcode-verification"

  lazy val library = (project in file("."))
    .enablePlugins(play.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning)
    .settings(
      name := appName,
      scalaVersion := "2.11.7",
      crossScalaVersions := Seq("2.11.7"),
      libraryDependencies ++= AppDependencies(),
      resolvers := Seq(
        Resolver.bintrayRepo("hmrc", "releases"),
        "typesafe-releases" at "http://repo.typesafe.com/typesafe/releases/"
      )
    )

}

private object AppDependencies {

  import play.PlayImport._
  import play.core.PlayVersion

  val compile = Seq(
    "com.typesafe.play" %% "play" % PlayVersion.current % "provided",
    ws % "provided",
    "uk.gov.hmrc" %% "play-authorised-frontend" % "5.5.0",
    "uk.gov.hmrc" %% "play-config" % "2.0.1",
    "uk.gov.hmrc" %% "play-auditing" % "1.9.0"
  )

  val testScope: String = "test"

  val test = Seq(
    "org.scalatest" %% "scalatest" % "2.2.1" % testScope,
    "org.pegdown" % "pegdown" % "1.4.2" % testScope,
    "uk.gov.hmrc" %% "hmrctest" % "1.4.0" % testScope

  )

  def apply() = compile ++ test
}