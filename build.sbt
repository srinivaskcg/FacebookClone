name := "Facebook"
 
version := "1.0"

resolvers += Resolver.url("TypeSafe Ivy releases", url("http://dl.bintray.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)

libraryDependencies ++=
    Seq(
	"com.typesafe.akka" %% "akka-actor" % "2.3.9",
        "com.typesafe.akka" %% "akka-slf4j" % "2.3.9",
	"io.spray" %% "spray-routing" % "1.3.2",
        "io.spray" %% "spray-client" % "1.3.2",
        "io.spray" %% "spray-json" % "1.3.1",
        "com.typesafe" % "config" % "1.2.1",
        "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
	"org.json4s" %% "json4s-jackson" % "3.2.11",
	"org.json4s" %% "json4s-ext" % "3.2.11",
	"org.json4s" %% "json4s-native" % "3.2.11",
	"org.json4s" %% "json4s-core" % "3.2.11",
	"commons-codec" % "commons-codec" % "1.9"
    )
