import Dependencies._

ThisBuild / scalaVersion     := "2.13.12"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "FunctionalProgProject",
    libraryDependencies += munit % Test,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.15" % Test,
    
    // HTTP client for API requests - FIXED
    libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.9.0",
    libraryDependencies += "com.softwaremill.sttp.client3" %% "akka-http-backend" % "3.9.0",
    
    // JSON parsing
    libraryDependencies += "io.circe" %% "circe-core" % "0.14.6",
    libraryDependencies += "io.circe" %% "circe-generic" % "0.14.6",
    libraryDependencies += "io.circe" %% "circe-parser" % "0.14.6",
    
    // Date/time handling
    libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.32.0",
    
    // Akka dependencies for STTP backend
    libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.8.0",
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.8.0",
    
    // Logging
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.13"
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
