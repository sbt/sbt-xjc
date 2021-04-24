## sbt-xjc An SBT Plugin to compile XML Schemata to JAXB Java sources with XJC

[JAXB - Java Architecture for XML Binding](https://javaee.github.io/jaxb-v2/)

[XJC documentation](https://javaee.github.io/jaxb-v2/doc/user-guide/ch04.html#tools-xjc)

XJC plugins
* [JAXB2 Commons](https://github.com/javaee/jaxb2-commons) 
* [JAXB XJC extended contract generation](https://mklemm.github.io/jaxb2-rich-contract-plugin/)

### Usage

Depend on the plugin: `./project/plugins.sbt`. Requires sbt 1.0.x

```
addSbtPlugin("org.scala-sbt.plugins" % "sbt-xjc" % "0.10")
```

### Configure

By default, all XSDs found under `unmanagedResourceDirectories` will be compiled. This is repeated in the
`Compile` and `Test` scopes. The table below show the configuration for the `Compile` scope;
replace with `Test` to configure that scope.

<table>
  <tr>
    <th>Key</th><th>Type</th><th>Default</th><th>Description</th><th>Example</th>
  </tr>
  <tr>
    <td>xjcLibs</td><td>Seq[ModuleId]</td><td>jaxb-api 2.1, jaxb-impl and jaxb-xjc 2.1.11</td>
    <td>The artifacts to download to run XJC</td>
    <td></td>
  </tr>
  <tr>
    <td>xjcPlugins</td><td>Seq[ModuleId]</td><td></td><td>The artifacts to download containing XJC plugins</td>
    <td></td>
  </tr>
  <tr>
    <td>xjcBindings</td><td>Seq[String]</td><td></td><td>Files used to customize JAXB bindings</td>
    <td></td>
  </tr>
  <tr>
    <td>xjcCommandLine</td><td>Seq[String]</td><td></td><td>Additional command line, e.g. -verbose -Xfluent-api</td>
    <td></td>
  </tr>
  <tr>
    <td>xjcJvmOpts</td><td>Seq[String]</td><td></td><td>Additional JVM command line, e.g. -Djavax.xml.accessExternalSchema=file to allow compilation of schemas consisting of multiple files</td>
    <td></td>
  </tr>
  <tr>
    <td>sources in (Compile, xjc)</td><td>Seq[File]</td><td>${unmanagedResourceDirectories} ** "*.xsd"</td><td>Input XSD Files</td>
    <td>sources in (Compile, xjc) &lt;&lt;= sourceDirectory map (_ / "main" / "schema" ** "*.xsd" get)

  </tr>
  <tr>
    <td>sourceManaged in (Compile, xjc)</td><td>File</td><td>${sourceManaged}/compile/xjc</td>
    <td>Target for generated files. Should not be shared with other generated files</td>
    <td></td>
  </tr>
</table>

Example `build.sbt` files from the tests:

* [Simple](./src/sbt-test/sbt-xjc/simple/build.sbt)
* [Fluent API](./src/sbt-test/sbt-xjc/fluent/build.sbt)
* [JAXB bindings](./src/sbt-test/sbt-xjc/xero/build.sbt)

Other samples may appear in [Tests](./src/sbt-test/sbt-xjc) for example builds.

As a convenience, the fluent API settings are provided in `SbtXjcPlugin.fluentApiSettings`

### Use

The timestamps of the source XSD files is compared with the generated files, and if newer these are compiled. This
occurs automatically before compilation.

### Problems

You can troubleshoot problems by inspecting debug logs with `> last xjc`.

Please use the Issue Tracker here if you find a bug. Do not raise bugs for problems with your schema or with usage of XJC itself.
