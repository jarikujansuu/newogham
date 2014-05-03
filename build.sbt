scalaVersion := "2.10.4"

name := "Ogham"

libraryDependencies ++= Seq(
	"com.github.nscala-time" %% "nscala-time" % "0.8.0",
	"com.typesafe" % "config" % "1.2.0",
	"com.stackmob" %% "newman" % "1.3.5",
	"org.scalatest" %% "scalatest" % "2.1.5" % "test"
)

mainClass in Compile := Some("jk.oe.invoicing.Invoicing")

retrieveManaged := true
