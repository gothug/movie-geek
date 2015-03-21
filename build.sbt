import sbtassembly.Plugin.AssemblyKeys._

name := "movie-geek"

scalaVersion := "2.11.6"

resolvers += "spray repo" at "http://repo.spray.io"

mainClass := Some("mvgk.httpservice.DockedServer")

val sprayVersion = "1.3.1-20140423"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.2",
  "io.spray" %% "spray-client" % sprayVersion,
  "io.spray" %% "spray-can" % sprayVersion,
  "io.spray" %% "spray-routing" % sprayVersion,
  "io.spray" %% "spray-json" % "1.2.6",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "org.jsoup" % "jsoup" % "1.8.1",
  "net.sourceforge.htmlunit" % "htmlunit" % "2.15",
  "org.seleniumhq.selenium" % "selenium-java" % "2.44.0" % "test",
  ("org.seleniumhq.selenium" % "selenium-firefox-driver" % "2+").exclude("net.java.dev.jna", "platform"),
  "com.typesafe.slick" %% "slick" % "2+",
  "com.typesafe.slick" %% "slick-codegen" % "2+",
  "com.github.tminglei" %% "slick-pg" % "0.8.2",
  "org.liquibase" % "liquibase-core" % "2.0.5"
)

assemblySettings

releaseSettings
