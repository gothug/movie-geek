import sbtassembly.Plugin.AssemblyKeys._

name := "movie-geek"

scalaVersion := "2.11.6"

resolvers += "spray repo" at "http://repo.spray.io"

mainClass := Some("mvgk.httpservice.DockedServer")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "io.spray" %% "spray-client"  % "1.3.2",
  "io.spray" %% "spray-can"     % "1.3.2",
  "io.spray" %% "spray-routing" % "1.3.2",
  "io.spray" %% "spray-json"    % "1.3.1",
  "io.spray" %% "spray-caching" % "1.3.1",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "org.jsoup" % "jsoup" % "1.8.1",
  "net.sourceforge.htmlunit" % "htmlunit" % "2.15",
  "org.seleniumhq.selenium"  % "selenium-java" % "2.45.0" % "test",
  ("org.seleniumhq.selenium" % "selenium-firefox-driver" % "2.45.0").exclude("net.java.dev.jna", "platform"),
  "com.typesafe.slick"  %% "slick" % "2.1.0",
  "com.typesafe.slick"  %% "slick-codegen" % "2.1.0",
  "com.github.tminglei" %% "slick-pg" % "0.8.2",
  "org.liquibase" % "liquibase-core" % "2.0.5"
)

assemblySettings

releaseSettings
