resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/snapshots/"

libraryDependencies <+= (sbtVersion).apply { sv =>
  "org.scala-sbt" % "scripted-plugin" % sv
}

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.2")
