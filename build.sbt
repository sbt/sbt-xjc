import sbt._
import Keys._
import bintray._

val sbtXjc = Project(
  id = "sbt-xjc",
  base = file("."),
  settings = Defaults.defaultSettings ++ Seq(
    licenses += ("BSD 3-Clause", url("http://opensource.org/licenses/BSD-3-Clause")),
    organization := "org.scala-sbt.plugins",
    version := "0.9-SNAPSHOT",
    sbtPlugin := true,
    publishMavenStyle := false,
    bintrayOrganization := Some("sbt"),
    bintrayRepository := "sbt-plugin-releases",
    bintrayPackage := "sbt-xjc-imported"
  ) ++ ScriptedPlugin.scriptedSettings
)
