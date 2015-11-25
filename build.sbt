name := "Facebook"

version := "0.1"

scalaVersion := "2.11.7"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies ++= {
	val akkaVersion = "2.3.9"
	val sprayVersion = "1.3.3"
	Seq("com.typesafe.akka" %% "akka-actor"     % akkaVersion,
		"io.spray"          %% "spray-can"      % sprayVersion,
		"io.spray"          %% "spray-routing"  % sprayVersion,
		"io.spray"          %% "spray-json"     % "1.3.2" ,
		"io.spray"          %%  "spray-client"  % sprayVersion,
	    "org.json4s"        %%  "json4s-native" % "3.3.0")
		}