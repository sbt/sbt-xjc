package com.github.retronym.sbtxjc

import java.io.File

import sbt.Keys._
import sbt._

/**
 * Compile Xml Schemata with JAXB XJC.
 */
object SbtXjcPlugin extends Plugin {
  /** An Ivy scope for the XJC compiler */
  val XjcTool   = config("xjc-tool").hide

  /** An Ivy scope for XJC compiler plugins, such as the Fluent API plugin */
  val XjcPlugin = config("xjc-plugin").hide

  val xjc            = TaskKey[Seq[File]]("xjc", "Generate JAXB Java sources from XSD files(s)")
  val xjcLibs        = SettingKey[Seq[ModuleID]]("xjc-libs", "Core XJC libraries")
  val xjcPlugins     = SettingKey[Seq[ModuleID]]("xjc-plugins", "Plugins for XJC code generation")
  val xjcCommandLine = SettingKey[Seq[String]]("xjc-plugin-command-line", "Extra command line parameters to XJC. Can be used to enable a plugin.")
  val xjcBindings    = SettingKey[Seq[String]]("xjc-plugin-bindings", "Binding files to add to XJC.")

  /** Main settings to enable XSD compilation */
  val xjcSettings     = Seq[Def.Setting[_]](
    ivyConfigurations ++= Seq(XjcTool, XjcPlugin),
    xjcCommandLine    := Seq(),
    xjcBindings       := Seq(),
    xjcPlugins        := Seq(),
    xjcLibs           := Seq(
      "org.glassfish.jaxb" % "jaxb-xjc" % "2.2.11"
    ),
    libraryDependencies <++= (xjcLibs)(_.map(_ % XjcTool.name)),
    libraryDependencies <++= (xjcPlugins)(_.map(_ % XjcPlugin.name))
  ) ++ xjcSettingsIn(Compile) ++ xjcSettingsIn(Test)

  /** Settings to enable the Fluent API plugin, that provides `withXxx` methods, in addition to `getXxx` and `setXxx`
   *  Requires this resolver http://download.java.net/maven/2/
   **/
  val fluentApiSettings = Seq[Def.Setting[_]](
    xjcPlugins     += "net.java.dev.jaxb2-commons" % "jaxb-fluent-api" % "2.1.8",
    xjcCommandLine += "-Xfluent-api"
  )

  def xjcSettingsIn(conf: Configuration): Seq[Def.Setting[_]] =
    inConfig(conf)(xjcSettings0) ++ Seq(clean <<= clean.dependsOn(clean in xjc in conf))

  /**
   * Unscoped settings, do not use directly, instead use `xjcSettingsIn(IntegrationTest)`
   */
  private def xjcSettings0 = Seq[Def.Setting[_]](
    sources in xjc       <<= unmanagedResourceDirectories.map(dirs => (dirs ** "*.xsd").get),
    sourceManaged in xjc ~= (_ / "xjc"), // e.g. /target/scala-2.8.1.final/src_managed/main/xjc
    xjc                  <<= (javaHome, classpathTypes in xjc, update, sources in xjc,
                              sourceManaged in xjc, xjcCommandLine, xjcBindings, streams).map(xjcCompile),
    sourceGenerators     <+= xjc,
    clean in xjc         <<= (sourceManaged in xjc, streams).map(xjcClean)
  )

  /**
   * @return the .java files in `sourceManaged` after compilation.
   */
  private def xjcCompile(javaHome: Option[File], classpathTypes: Set[String], updateReport: UpdateReport,
                         xjcSources: Seq[File], sourceManaged: File, extraCommandLine: Seq[String], 
						 xjcBindings: Seq[String], streams: TaskStreams): Seq[File] = {
    import streams.log
    def generated = (sourceManaged ** "*.java").get

    val shouldProcess = (xjcSources, generated) match {
      case (Seq(), _)  => false
      case (_, Seq())  => true
      case (ins, outs) =>
        val inLastMod = ins.map(_.lastModified()).max
        val outLasMod = outs.map(_.lastModified()).min
        outLasMod < inLastMod
    }

    lazy val options: Seq[String] = {
      import File.pathSeparator
      def jars(config: Configuration): Seq[File] = Classpaths.managedJars(config, classpathTypes, updateReport).map(_.data)
      val pluginJars      = jars(XjcPlugin)
      val mainJars        = jars(XjcTool)
      val jvmCpOptions    = Seq("-classpath", mainJars.mkString(pathSeparator))
      val xsdSourcePaths  = xjcSources.map(_.getAbsolutePath)
      val pluginCpOptions = pluginJars match {
        case Seq() => Seq()
        case js    => Seq("-extension", "-classpath", js.mkString(pathSeparator))
      }
      val appOptions = pluginCpOptions ++ Seq("-d", sourceManaged.getAbsolutePath)
      val mainClass  = "com.sun.tools.xjc.XJCFacade"
	  val bindings = xjcBindings.map(List("-b",_)).flatten

      jvmCpOptions ++ List(mainClass) ++ appOptions ++ extraCommandLine ++ xsdSourcePaths ++ bindings
    }

    if (shouldProcess) {
      sourceManaged.mkdirs()
      log.info("Compiling %d XSD file(s) to %s".format(xjcSources.size, sourceManaged.getAbsolutePath))
      log.debug("XJC java command line: " + options.mkString("\n"))
      val returnCode = Forker(javaHome, options, log)
      if (returnCode != 0) sys.error("Non zero return code from xjc [%d]".format(returnCode))
    } else {
      log.debug("No sources newer than products, skipping.")
    }

    generated
  }

  private def xjcClean(sourceManaged: File, streams: TaskStreams) {
    import streams.log
    val filesToDelete = (sourceManaged ** "*.java").get
    log.debug("Cleaning Files:\n%s".format(filesToDelete.mkString("\n")))
    if (filesToDelete.nonEmpty) {
      log.info("Cleaning %d XJC generated files in %s".format(filesToDelete.size, sourceManaged.getAbsolutePath))
      IO.delete(filesToDelete)
    }
  }
}
