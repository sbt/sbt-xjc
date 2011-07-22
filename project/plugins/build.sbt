resolvers += Resolver.url("Typesafe repository", new java.net.URL("http://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.defaultIvyPatterns)

libraryDependencies <+= sbtVersion(_ => "org.scala-tools.sbt" %% "scripted-plugin" % "0.10.1")