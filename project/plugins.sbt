logLevel := Level.Warn

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.3")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
