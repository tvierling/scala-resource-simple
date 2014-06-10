name := "scala-resource-simple"

version := "0.1"

scalaVersion := "2.11.1"

crossScalaVersions := Seq("2.11.1", "2.10.4")

scalacOptions in ThisBuild ++= Seq("-feature", "-optimise", "-deprecation", "-target:jvm-1.6")
