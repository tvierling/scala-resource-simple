organization := "org.duh"

name := "scala-resource-simple"

version := "0.4-SNAPSHOT"

scalaVersion := "2.11.5"

crossScalaVersions := Seq("2.11.5", "2.10.4")

scalacOptions in ThisBuild ++= Seq("-feature", "-optimise", "-deprecation", "-target:jvm-1.6")

site.settings

site.includeScaladoc()

ghpages.settings

git.remoteRepo := "git@github.com:tvierling/scala-resource-simple.git"

licenses := Seq("Unlicense" -> url("http://unlicense.org/"))

homepage := Some(url("http://tvierling.github.io/scala-resource-simple"))

usePgpKeyHex("c97da2c45fc249fd")

useGpg := true

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <scm>
    <url>git@github.com:tvierling/scala-resource-simple.git</url>
    <connection>scm:git:git@github.com:tvierling/scala-resource-simple.git</connection>
  </scm>
  <developers>
    <developer>
      <id>tvierling</id>
      <name>Todd Vierling</name>
      <url>http://www.duh.org/</url>
    </developer>
  </developers>)
