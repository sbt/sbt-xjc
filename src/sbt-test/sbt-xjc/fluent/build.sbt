import com.github.retronym.sbtxjc.SbtXjcPlugin
import sbt._
import Keys._

SbtXjcPlugin.fluentApiSettings

resolvers += "Java Net" at "http://download.java.net/maven/2/"
