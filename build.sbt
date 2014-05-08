scalaVersion := "2.10.4"

name := "New Ogham League Browser"

unmanagedSourceDirectories in Compile := (scalaSource in Compile).value :: Nil

unmanagedSourceDirectories in Test := (scalaSource in Test).value :: Nil

libraryDependencies ++= Seq(
	"com.github.nscala-time" %% "nscala-time" % "1.0.0",
	"com.typesafe" % "config" % "1.2.0",
	"com.stackmob" %% "newman" % "1.3.5",
	"net.databinder" %% "unfiltered-filter" % "0.7.1",
  "net.databinder" %% "unfiltered-jetty" % "0.7.1",
	"javax.servlet" % "javax.servlet-api" % "3.0.1",
	"org.json4s" %% "json4s-native" % "3.2.9",
	"org.json4s" %% "json4s-ext" % "3.2.9",
	"org.json4s" %% "json4s-scalaz" % "3.2.9",
	"com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
	"log4j" % "log4j" % "1.2.17",
	"org.scalatest" %% "scalatest" % "2.1.5" % "test"
)

mainClass in Compile := Some("org.newogham.Server")

retrieveManaged := true
