package com.github.retronym.sbtxjc

import sbt.Fork.ForkJava
import java.io.File
import sbt.Logger

// TODO references ForkJava triggers a SOE in IDEA, probably because of scalap.
object Forker {
  def apply(javaHome: Option[File], options: Seq[String], log: Logger): Int = (new ForkJava("java")).apply(javaHome, options, log)
}