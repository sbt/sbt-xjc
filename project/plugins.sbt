resolvers += Resolver.url("Typesafe repository", new java.net.URL("http://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.defaultIvyPatterns)

resolvers += Resolver.url("Typesafe snapshot repository", new java.net.URL("http://typesafe.artifactoryonline.com/typesafe/ivy-snapshots/"))(Resolver.defaultIvyPatterns)

libraryDependencies <+= (sbtVersion).apply { sv =>
  "org.scala-sbt" % "scripted-plugin" % sv
}

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.2")