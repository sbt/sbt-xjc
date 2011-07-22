import sbt._
import Keys._

object build extends Build {
  val sbtXjc = Project(
    id = "sbt-xjc",
    base = file("."),
    settings = Defaults.defaultSettings ++ ScriptedPlugin.scriptedSettings ++ Seq(
      organization := "com.github.retronym",
      version := "0.1-SNAPSHOT",
      sbtPlugin := true
    )
  )
}