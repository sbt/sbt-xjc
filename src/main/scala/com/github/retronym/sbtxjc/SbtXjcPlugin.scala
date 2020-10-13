package com.github.retronym.sbtxjc

import java.io.File

import sbt._
import Keys._

/**
 * Compile Xml Schemata with JAXB XJC.
 */
object SbtXjcPlugin extends AutoPlugin {

  override def requires = sbt.plugins.JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    val xjc            = TaskKey[Seq[File]]("xjc", "Generate JAXB Java sources from XSD files(s)")
    val xjcLibs        = SettingKey[Seq[ModuleID]]("xjc-libs", "Core XJC libraries")
    val xjcPlugins     = SettingKey[Seq[ModuleID]]("xjc-plugins", "Plugins for XJC code generation")
    val xjcJvmOpts     = SettingKey[Seq[String]]("xjc-jvm-opts", "Extra command line parameters to the JVM XJC runs in.")
    val xjcCommandLine = SettingKey[Seq[String]]("xjc-plugin-command-line", "Extra command line parameters to XJC. Can be used to enable a plugin.")
    val xjcBindings    = SettingKey[Seq[String]]("xjc-plugin-bindings", "Binding files to add to XJC.")
  }
  import autoImport._

  /** An Ivy scope for the XJC compiler */
  val XjcTool   = config("xjc-tool").hide

  /** An Ivy scope for XJC compiler plugins, such as the Fluent API plugin */
  val XjcPlugin = config("xjc-plugin").hide

  /** Main settings to enable XSD compilation */
    override lazy val projectSettings = Seq[Def.Setting[_]](
    ivyConfigurations ++= Seq(XjcTool, XjcPlugin),
    xjcCommandLine    := Seq(),
    xjcJvmOpts        := Seq(),
    xjcBindings       := Seq(),
    xjcPlugins        := Seq(),
    xjcLibs           := Seq(
      "org.glassfish.jaxb" % "jaxb-xjc"  % "2.2.11",
      "com.sun.xml.bind"   % "jaxb-impl" % "2.2.11",
      "javax.activation" % "activation" % "1.1.1"
    ),
    libraryDependencies ++= xjcLibs.value.map(_ % XjcTool.name),
    libraryDependencies ++= xjcPlugins.value.map(_ % XjcPlugin.name)
  ) ++ xjcSettingsIn(Compile) ++ xjcSettingsIn(Test)

  /** Settings to enable the Fluent API plugin, that provides `withXxx` methods, in addition to `getXxx` and `setXxx`
   *  Requires this resolver http://download.java.net/maven/2/
   **/
  val fluentApiSettings = Seq[Def.Setting[_]](
    xjcPlugins     += "net.java.dev.jaxb2-commons" % "jaxb-fluent-api" % "2.1.8",
    xjcCommandLine += "-Xfluent-api"
  )

  def xjcSettingsIn(conf: Configuration): Seq[Def.Setting[_]] =
    inConfig(conf)(xjcSettings0) ++ Seq(clean := clean.dependsOn(clean in xjc in conf).value)

  /**
   * Unscoped settings, do not use directly, instead use `xjcSettingsIn(IntegrationTest)`
   */
  private def xjcSettings0 = Seq[Def.Setting[_]](
    sources in xjc       := unmanagedResourceDirectories.value.flatMap(dirs => (dirs ** "*.xsd").get),
    sourceManaged in xjc := crossTarget.value / "src_managed_cxf", // This directory structure was recommended in https://github.com/sbt/sbt/issues/1664#issuecomment-213057686
    xjc                  := xjcCompile(javaHome.value, (classpathTypes in xjc).value, update.value, (sources in xjc).value,
                             (sourceManaged in xjc).value, xjcCommandLine.value, xjcJvmOpts.value, xjcBindings.value, streams.value),
    sourceGenerators     += xjc,
    clean in xjc         := xjcClean((sourceManaged in xjc).value, streams.value),
    managedSourceDirectories in Compile += (sourceManaged in xjc).value
  )

  /**
   * @return the .java files in `sourceManaged` after compilation.
   */
  private def xjcCompile(javaHome: Option[File], classpathTypes: Set[String], updateReport: UpdateReport,
                         xjcSources: Seq[File], sourceManaged: File, extraCommandLine: Seq[String], xjcJvmOpts: Seq[String],
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
      jvmCpOptions ++ xjcJvmOpts ++ List(mainClass) ++ appOptions ++ extraCommandLine ++ xsdSourcePaths ++ bindings
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
