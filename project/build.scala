import sbt._
import Keys._

object build extends Build {
  val sbtXjc = Project(
    id = "sbt-xjc",
    base = file("."),
    settings = Defaults.defaultSettings ++ Seq(
      organization := "com.github.retronym",
      version := "0.6-SNAPSHOT",
      sbtPlugin := true,
      publishTo := Some(Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)),
      publishMavenStyle := false
    )
  )
}