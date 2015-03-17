## sbt-xjc An SBT Plugin to compile XML Schemata to JAXB Java sources with XJC

### Usage

Depend on the plugin: `./project/plugins/build.sbt`. Requires SBT 0.12.x or 0.13.x

```
addSbtPlugin("org.scala-sbt.plugins" % "sbt-xjc" % "0.6")
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
    <td>xjcLibs</td><td>Seq[ModuleId]</td><td>jaxb-api 2.1, jaxb-impl and jaxb-xjc 2.1.9</td>
    <td>The artifacts to download to run XJC</td>
    <td></td>
  </tr>
  <tr>
    <td>xjcPlugins</td><td>Seq[ModuleId]</td><td></td><td>The artifacts to download containing XJC plugins</td>
    <td></td>
  </tr>
  <tr>
    <td>xjcCommandLine</td><td>Seq[String]</td><td></td><td>Additional command line, e.g. -verbose -Xfluent-api</td>
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

See the [Tests](https://github.com/retronym/sbt-xjc/tree/master/src/sbt-test/sbt-xjc) for example builds.

As a convenience, the fluent API settings are provided in `SbtXjcPlugin.fluentApiSettings`

### Use

The timestamps of the source XSD files is compared with the generated files, and if newer these are compiled. This
occurs automatically before compilation.

### Problems

You can troubleshoot problems by inspecting debug logs with `> last xjc`.

Please use the Issue Tracker here if you find a bug. Do not raise bugs for problems with your schema or with usage of XJC itself.
