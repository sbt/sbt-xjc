package com.github.retronym.sbtxjc

import sbt._
import Keys._
import sbt.Fork.ForkJava
import java.io.File

/**
 * Compile Xml Schemata with JAXB XJC.
 */
// TODO Could we support compile both test and compile schema?
object SbtXjcPlugin {
  /** The main scope for this plugin, used to mark the library dependencies on the XJC compiler, and to scope tasks and settings */
  val Xjc       = config("xjc").hide

  /** An Ivy scope for XJC compiler plugins, such as the Fluent API plugin */
  val XjcPlugin = config("xjc-plugin").hide

  val xjcLibs         = SettingKey[Seq[ModuleID]]("xjc-libs", "Core XJC libraries")
  val xjcPlugins      = SettingKey[Seq[ModuleID]]("xjc-plugins", "Plugins for XJC code generation")
  val xjcCommandLine  = SettingKey[Seq[String]]("xjc-plugin-command-line", "Extra command line parameters to XJC. Can be used to enable a plugin.")
  val xjcCompile      = TaskKey[Seq[File]]("xjc-compile", "Generate JAXB Java sources from XSD files(s)")
  // Other configuration:
  // sources in Xjc        The XSD files compiled by XSJ
  // sourceManaged in Xjc  The output directory for generated Java files.


  /** Settings to enable the Fluent API plugin, that provides `withXxx` methods, in addition to `getXxx` and `setXxx`
   *  Requires this resolver http://download.java.net/maven/2/
   **/
  val fluentApiSettings = Seq[Project.Setting[_]](
    xjcPlugins     += "net.java.dev.jaxb2-commons" % "jaxb-fluent-api" % "2.1.8",
    xjcCommandLine += "-Xfluent-api"
  )

  /** Main settings to enable XSD compilation */
  val xjcSettings     = Seq[Project.Setting[_]](
    ivyConfigurations ++= Seq(Xjc, XjcPlugin),
    xjcCommandLine    := Seq(),
    xjcPlugins        := Seq(),
    xjcLibs           := Seq(
      "com.sun.xml.bind" % "jaxb-impl" % "2.1.9",
      "com.sun.xml.bind" % "jaxb-xjc" % "2.1.9",
      "javax.xml.bind" % "jaxb-api" % "2.1"
    ),

    sources in Xjc              <<= (unmanagedResourceDirectories in Compile).map{ (sm: Seq[File]) => sm.flatMap(s => (s ** "*.xsd").get) },
    libraryDependencies         <++= (xjcLibs){ _.map(_ % Xjc.name) },
    libraryDependencies         <++= (xjcPlugins){ _.map(_ % XjcPlugin.name) },
    sourceManaged in Xjc        <<= sourceManaged(_ / "xjc"),
    sourceGenerators in Compile <+= xjcCompile.identity,
    clean in Xjc                <<= (sourceManaged in Xjc, streams).map {
      (sm, s) =>
        val filesToDelete = (sm ** "*").get
        s.log.debug("Cleaning: " + filesToDelete)
        IO.delete(filesToDelete)
    },
    clean <<= (clean).dependsOn(clean in Xjc),
    xjcCompile <<= (javaHome, classpathTypes in Xjc, update, sources in Xjc, sourceManaged in Xjc, xjcCommandLine, streams).map(xjcCompile)
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
      import File.pathSeparator
      def jars(config: Configuration): Seq[File] = Classpaths.managedJars(config, classpathTypes, updateReport).map(_.data)
      val pluginJars      = jars(XjcPlugin)
      val mainJars        = jars(Xjc)
      val jvmCpOptions    = Seq("-classpath", mainJars.mkString(pathSeparator))
      val xsdSourcePaths  = xjcSources.map(_.getAbsolutePath)
      val pluginCpOptions = pluginJars match {
        case Seq() => Seq()
        case js    => Seq("-extension", "-classpath", js.mkString(pathSeparator))
      }
      val appOptions = pluginCpOptions ++ Seq("-d", sourceManaged.getAbsolutePath)
      val mainClass  = "com.sun.tools.xjc.XJCFacade"

      jvmCpOptions ++ List(mainClass) ++ appOptions ++ cl ++ xsdSourcePaths
    }

    if (shouldProcess) {
      sourceManaged.mkdirs()
      s.log.debug("XJC java command line: " + options.mkString("\n"))
      val returnCode = (new ForkJava("java")).apply(javaHome, options, s.log)
      if (returnCode != 0) error("Non zero return code from xjc [%d]".format(returnCode))
    } else {
      s.log.debug("No sources newer than products, skipping.")
    }

    generated
  }
}
