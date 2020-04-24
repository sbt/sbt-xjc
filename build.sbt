import sbt._
import Keys._
import bintray._

lazy val sbtXjc = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    organization := "org.scala-sbt.plugins",
    name := "sbt-xjc",
    version := "0.10.1-SNAPSHOT",

    sbtPlugin := true,

    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,

    licenses += ("BSD 3-Clause", url(
      "http://opensource.org/licenses/BSD-3-Clause")),
    publishMavenStyle := false,
    bintrayOrganization := Some("sbt"),
    bintrayRepository := "sbt-plugin-releases",
    bintrayPackage := "sbt-xjc-imported"
  )
