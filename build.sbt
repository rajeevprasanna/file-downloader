name := "SFDCAnalytics"

version := "1.0"

scalaVersion := "2.12.1"


addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats" % "0.9.0",
  "org.typelevel" %% "dogs-core" % "0.5.2",
  "com.typesafe.akka" %% "akka-actor" % "2.5.3",
  "com.typesafe.akka" %% "akka-stream" % "2.5.3",
  "com.typesafe.akka" %% "akka-http" % "10.0.9",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.9"
)



