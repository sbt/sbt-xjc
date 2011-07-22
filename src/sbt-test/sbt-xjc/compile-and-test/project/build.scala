import com.github.retronym.sbtxjc.test.BaseScriptedTestBuild
import com.github.retronym.sbtxjc.SbtXjcPlugin
import sbt._
import Keys.libraryDependencies

object build extends BaseScriptedTestBuild {
  lazy val root = Project("main", file("."), settings = Defaults.defaultSettings ++ scriptedTestSettings ++ SbtXjcPlugin.xjcSettings ++ Seq(
    SbtXjcPlugin.xjcCommandLine += "-verbose"
  ))
}