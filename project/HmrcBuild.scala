import sbt.Keys._
import sbt._

object HmrcBuild extends Build {

  import uk.gov.hmrc._
  import uk.gov.hmrc.versioning.SbtGitVersioning

  val appName = "passcode-verification"

  lazy val library = (project in file("."))
    .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning)
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

  import play.sbt.PlayImport._
  import play.core.PlayVersion

  val compile = Seq(
    "com.typesafe.play" %% "play" % PlayVersion.current % "provided",
    ws % "provided",
    "uk.gov.hmrc" %% "play-authorised-frontend" % "6.3.0",
    "uk.gov.hmrc" %% "play-config" % "4.3.0",
    "uk.gov.hmrc" %% "play-auditing" % "2.10.0"
  )

  val testScope: String = "test"

  val test = Seq(
    "org.scalatest" %% "scalatest" % "2.2.6" % testScope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % testScope,
    "org.pegdown" % "pegdown" % "1.4.2" % testScope,
    "uk.gov.hmrc" %% "hmrctest" % "2.3.0" % testScope

  )

  def apply() = compile ++ test
}
