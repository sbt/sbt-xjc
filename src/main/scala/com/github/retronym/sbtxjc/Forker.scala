package com.github.retronym.sbtxjc

import java.io.File

import sbt.{Fork, ForkOptions, Logger}

object Forker {
  def apply(javaHome: Option[File], options: Seq[String], log: Logger): Int = {
    new Fork("java", None).apply(new ForkOptions(javaHome = javaHome), options)
  }
}
