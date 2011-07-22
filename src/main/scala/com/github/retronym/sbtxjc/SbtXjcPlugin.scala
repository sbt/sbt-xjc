package com.github.retronym.sbtxjc

import sbt._
import Keys._
import sbt.Fork.ForkJava
import java.io.File
import xsbti.api.Val

/**
 * Compile Xml Schemata with JAXB XJC.
 */
// TODO Add 'xjc' scope and reuse SBT keys, where appropriate.
// TODO Could we support compile both test and compile schema?
object SbtXjcPlugin {
  val xjcConfig       = config("xjc").hide
  val xjcPluginConfig = config("xjc-plugin").hide

  val xsdCompile      = TaskKey[Seq[File]]("xsd-compile", "Compiles XML Schema file(s) with XJC to generated Java sources")
  val xjcLibs         = SettingKey[Seq[ModuleID]]("xjc-libs", "Core XJC libraries")
  val xjcPlugins      = SettingKey[Seq[ModuleID]]("xjc-plugins", "Plugins for XJC code generation")
  val xjcSources      = SettingKey[Seq[File]]("xjc-sources", "Source XSD files")
  val xjcCommandLine  = SettingKey[Seq[String]]("xjc-plugin-command-line", "Extra command line parameters to XJC. Can be used to enable a plugin.")
  val xjcGenerated    = SettingKey[(File => Seq[File])]("xjc-generataed", "A function to locate generated Java files, relative to the given ")
  val xjcClean        = TaskKey[Unit]("xjc-clean", "Cleans XJC generated sources")

  /** Settings to enable the Fluent API plugin, that provides `withXxx` methods, in addition to `getXxx` and `setXxx`
   *  Requires this resolver http://download.java.net/maven/2/
   **/
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
      "com.sun.xml.bind" % "jaxb-impl" % "2.1.9",
      "com.sun.xml.bind" % "jaxb-xjc" % "2.1.9",
      "javax.xml.bind" % "jaxb-api" % "2.1"
    ),

    xjcSources in Compile       <<= (unmanagedResourceDirectories in Compile){ (sm: Seq[File]) => sm.flatMap(s => (s ** "*.xsd").get) },
    libraryDependencies         <++= (xjcLibs){ _.map(_ % xjcConfig.name) },
    libraryDependencies         <++= (xjcPlugins){ _.map(_ % xjcPluginConfig.name) },
    sourceManaged in xsdCompile <<= sourceManaged(_ / "xjc"),
    sourceGenerators in Compile <+= xsdCompile.identity,
    xjcClean <<= (sourceManaged in xsdCompile, streams).map {
      (sm, s) =>
        val filesToDelete = (sm ** "*").get
        s.log.debug("Cleaning: " + filesToDelete)
        IO.delete(filesToDelete)
    },
    clean <<= (clean).dependsOn(xjcClean),
    xsdCompile <<= (javaHome, classpathTypes in xsdCompile, update, xjcSources in Compile,
            sourceManaged in xsdCompile, xjcCommandLine, streams).map(xjcCompile)
  )

  private def xjcCompile(javaHome: Option[File], classpathTypes: Set[String], updateReport: UpdateReport,
                         xjcSources: Seq[File], sourceManaged: File, cl: Seq[String], s: TaskStreams): Seq[File] = {
    def generated = (sourceManaged ** "*.java").get

    val shouldProcess = (xjcSources, generated) match {
      case (Seq(), _) => false
      case (_, Seq()) => true
      case (ins, outs) =>
        val inLastMod = ins.map(_.lastModified()).max
        val outLasMod = outs.map(_.lastModified()).min
        outLasMod < inLastMod
    }

    lazy val options: Seq[String] = {
      val sep = File.pathSeparator
      def jars(config: Configuration): Seq[File] = Classpaths.managedJars(config, classpathTypes, updateReport).map(_.data)
      val pluginJars      = jars(xjcPluginConfig)
      val mainJars        = jars(xjcConfig)
      val jvmCpOptions    = Seq("-classpath", mainJars.mkString(sep))
      val xsdSourcePaths  = xjcSources.map(_.getAbsolutePath)
      val pluginCpOptions = pluginJars match {
        case Seq() => Seq()
        case js    => Seq("-classpath", js.mkString(sep))
      }
      val appOptions = Seq("-extension") ++ pluginCpOptions ++ Seq("-d", sourceManaged.getAbsolutePath)
      val mainClass  = "com.sun.tools.xjc.XJCFacade"

      jvmCpOptions ++ List(mainClass) ++ appOptions ++ cl ++ xsdSourcePaths
    }

    if (shouldProcess) {
      sourceManaged.mkdirs()
      // Workaround for SOE in IntelliJ, scalap should use _root_.scala.Option to disambiguate from Fork.java.
      type ForkApply = {def apply(x: Option[File], opts: Seq[String], log: Logger): Int}
      val forkJava = (Fork.java.asInstanceOf[ForkApply])
      val returnCode = (new ForkJava("java")).apply(javaHome, options, s.log)

      s.log.debug("XJC java command linee: " + options.mkString("\n"))
      if (returnCode != 0) error("Failed: %d".format(returnCode))
    } else {
      s.log.info("No sources newer than products, skipping.")
    }

    generated
  }
}
