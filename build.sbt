import sbt._
import Keys._
import bintray._

lazy val sbtXjc = (project in file("."))
  .settings(
    name := "sbt-xjc",
    licenses += ("BSD 3-Clause", url(
      "http://opensource.org/licenses/BSD-3-Clause")),
    organization := "org.scala-sbt.plugins",
    version := "0.10-SNAPSHOT",
    sbtPlugin := true,
    publishMavenStyle := false,
    bintrayOrganization := Some("sbt"),
    bintrayRepository := "sbt-plugin-releases",
    bintrayPackage := "sbt-xjc-imported"
  )
  .settings(
    crossSbtVersions := Seq("0.13.16", "1.0.2"),
    sbtVersion in Global := "0.13.16",
    scalaCompilerBridgeSource := {
      val sv = appConfiguration.value.provider.id.version
      ("org.scala-sbt" % "compiler-interface" % sv % "component").sources
    }
  )
