package com.github.retronym.sbtxjc
package test

import sbt._

abstract class BaseScriptedTestBuild extends Build {
	lazy val scriptedTestSettings = Seq[Project.Setting[_]]()
}
