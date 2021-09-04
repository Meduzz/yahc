name := "Yahc"

version := "1.6"

scalaVersion := "2.12.4"

organization := "se.kodiak.tools"

publishArtifact in (Compile, packageDoc) := false

libraryDependencies ++= Seq(
	"org.scalaj" %% "scalaj-http" % "2.4.1",
	"org.json4s" %% "json4s-native" % "3.6.2",
	"org.scalatest" %% "scalatest" % "3.0.0" % "test",
	"io.vertx" % "vertx-web" % "4.1.1" % "test"
)