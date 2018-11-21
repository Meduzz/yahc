name := "Yahc"

version := "1.2"

scalaVersion := "2.12.4"

organization := "se.kodiak.tools"

credentials += Credentials(Path.userHome / ".ivy2" / ".tools")

publishTo := Some("se.kodiak.tools" at "https://yamr.kodiak.se/maven")

publishArtifact in (Compile, packageDoc) := false

resolvers += "se.chimps.cameltow" at "https://yamr.kodiak.se/maven"

libraryDependencies ++= Seq(
	"org.scalaj" %% "scalaj-http" % "2.4.1",
	"org.scalatest" %% "scalatest" % "3.0.0" % "test",
	"org.json4s" %% "json4s-native" % "3.6.2",
	"se.chimps.cameltow" %% "cameltow" % "2.0-beta14" % "test"
)