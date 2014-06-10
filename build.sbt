organization := "org.duh"

name := "scala-resource-simple"

version := "0.3-SNAPSHOT"

scalaVersion := "2.11.1"

crossScalaVersions := Seq("2.11.1", "2.10.4")

scalacOptions in ThisBuild ++= Seq("-feature", "-optimise", "-deprecation", "-target:jvm-1.6")

site.settings

site.includeScaladoc()

ghpages.settings

git.remoteRepo := "git@github.com:tvierling/scala-resource-simple.git"
