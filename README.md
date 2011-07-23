## sbt-xjc An SBT 0.10 Plugin to compile XML Schemata to JAXB Java sources with XJC

### Usage

Depend on the plugin: `./project/plugins/build.sbt`

```
resolvers += "retronym" at "http://retronym.github.com/repo/releases"

libraryDependencies += "com.github.retronym" %% "sbt-xjc" % "0.2"
```

### Introduce Settings

Include the settings from `com.github.retronym.SbtXjcPlugin.settings`.

### Configure

By default, all XSDs found under `unmanagedResourceDirectories` will be compiled. This is repeated in the
`Compile` and `Test` scopes. The table below show the configuration for the `Compile` scope;
replace with `Test` to configure that scope.

<table>
  <tr>
    <th>Key</th><th>Type</th><th>Default</th><th>Description</th>
  </tr>
  <tr>
    <td>xjcLibs</td><td>Seq[ModuleId]</td><td>jaxb-api 2.1, jaxb-impl and jaxb-xjc 2.1.9</td><td>The artifacts to download to run XJC</td>
  </tr>
  <tr>
    <td>xjcPlugins</td><td>Seq[ModuleId]</td><td></td><td>The artifacts to download containing XJC plugins</td>
  </tr>
  <tr>
    <td>xjcCommandLine</td><td>Seq[String]</td><td></td><td>Additional command line, e.g. -verbose -Xfluent-api</td>
  </tr>
  <tr>
    <td>sources in (xjc, Compile)</td><td>Seq[File]</td><td>${unmanagedResourceDirectories} ** "*.xsd"</td><td>Input XSD Files</td>
  </tr>
  <tr>
    <td>sourceManaged in (xjc, Compile)</td><td>`File`</td><td>${sourceManaged}/compile/xjc</td><td>Target for generated files. Should not be shared with other generated files</td>
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