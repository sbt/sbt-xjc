package com.github.retronym.sbtxjc

import sbt._
import Keys._
import sbt.Fork.ForkJava

/**
 * Compile Xml Schemata with JAXB XJC.
 */
// TODO Review scoping. Could we compile test and compile schema?
// TODO Reuse SBT keys, where appropriate.
object SbtXjcPlugin {
  val xjcConfig       = config("xjc").hide
  val xjcPluginConfig = config("xjc-plugin").hide

  val xsdCompile      = TaskKey[Seq[File]]("xsd-compile", "Compiles XML Schema file(s) with XJC to generated Java sources")
  val xjcLibs         = SettingKey[Seq[ModuleID]]("xjc-plugins", "Core XJC libraries")
  val xjcPlugins      = SettingKey[Seq[ModuleID]]("xjc-plugins", "Plugins for XJC code generation")
  val xjcSources      = SettingKey[Seq[File]]("xjc-sources", "Source XSD files")
  val xjcCommandLine  = SettingKey[Seq[String]]("xjc-plugin-command-line", "Extra command line parameters to XJC. Can be used to enable a plugin.")

  /** Settings to enable the Fluent API plugin, that provides `withXxx` methods, in addition to `getXxx` and `setXxx` */
  val fluentApiSettings = Seq[Project.Setting[_]](
    xjcPlugins     += "net.java.dev.jaxb2-commons" % "jaxb-fluent-api" % "2.1.8",
    xjcCommandLine += "-Xfluent-api"
  )

  /** Main settings to enable XSD compilation */
  val xjcSettings     = Seq[Project.Setting[_]](
    ivyConfigurations ++= Seq(xjcConfig, xjcPluginConfig),
    xjcCommandLine    := Seq(),
    xjcPlugins        := Seq(),
    xjcLibs           := Seq(
      "com.sun.xml.bind" % "jaxb-impl" % "2.1.9" % xjcConfig.name,
      "com.sun.xml.bind" % "jaxb-xjc" % "2.1.9" % xjcConfig.name,
      "javax.xml.bind" % "jaxb-api" % "2.1" % xjcConfig.name
    ),

    xjcSources in Compile       <<= (sourceManaged in Compile){ sm => (sm ** "*.xsd").get },
    libraryDependencies         <++= (xjcLibs){ _.map(_ % xjcConfig.name) },
    libraryDependencies         <++= (xjcPlugins){ _.map(_ % xjcPluginConfig.name) },
    sourceGenerators in Compile <+= xsdCompile.identity,

    xsdCompile <<= (javaHome, classpathTypes in xsdCompile, update, xjcSources in Compile,
            resourceDirectory in Compile, xjcCommandLine, streams).map(xjcCompile)
  )

  private def xjcCompile(javaHome: Option[File], classpathTypes: Set[String], updateReport: UpdateReport,
                         xjcSources: Seq[File], out: File, cl: Seq[String], s: TaskStreams): Seq[File] = {
    val options: Seq[String] = {
      def jars(config: Configuration): Seq[File] = Classpaths.managedJars(config, classpathTypes, updateReport).map(_.data)
      val pluginJars      = jars(xjcPluginConfig)
      val mainJars        = jars(xjcConfig)
      val jvmCpOptions    = Seq("-classpath", mainJars.mkString(";"))
      val xsdSourcePaths  = xjcSources.map(_.getAbsolutePath)
      val pluginCpOptions = pluginJars match {
        case Seq() => Seq()
        case js    => Seq("-classpath", js.mkString(":"))
      }
      val appOptions = Seq.apply[String]("-extension") ++ pluginCpOptions ++ Seq("-d", out.getAbsolutePath)
      val mainClass  = "com.sun.tools.xjc.XJCFacade"

      jvmCpOptions ++ List(mainClass) ++ appOptions ++ cl ++ xsdSourcePaths
    }
    s.log.debug("XJC java command linee: " + options.mkString("\n"))

    // Workaround for SOE in IntelliJ, minimize and report.
    val forkJava: ForkJava = Fork.java
    type ForkApply = {def apply(x: Option[File], opts: Seq[String], log: Logger): Int}
    val returnCode = (forkJava: ForkApply).apply(javaHome, options, s.log)

    if (returnCode != 0) error("Failed: %d".format(returnCode))

    (out ** "*.java").get // TODO be more precise
  }
}
