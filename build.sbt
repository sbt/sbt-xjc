import sbt._
import Keys._

val sbtXjc = Project(
  id = "sbt-xjc",
  base = file("."),
  settings = Defaults.defaultSettings ++ Seq(
    organization := "org.scala-sbt.plugins",
    version := "0.8-SNAPSHOT",
    sbtPlugin := true,
    publishTo := Some(Resolver.url("sbt-plugin-releases-publish", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)),
    publishMavenStyle := false
  ) ++ ScriptedPlugin.scriptedSettings
)
