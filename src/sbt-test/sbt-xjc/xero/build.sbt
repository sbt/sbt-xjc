xjcCommandLine += "-verbose"

xjcCommandLine += "-p"

xjcCommandLine += "com.sbt.generated"

xjcBindings += "src/main/resources/bindings.xjb"

libraryDependencies += "jakarta.xml.bind" % "jakarta.xml.bind-api"  % "3.0.1"
