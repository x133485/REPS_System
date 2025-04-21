import scala.collection.immutable.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

lazy val root = (project in file("."))
  .settings(
    name := "WindForecastSystem",
    libraryDependencies ++= Seq(
      //"com.lihaoyi" %% "requests" % "0.8.3",
      "com.lihaoyi" %% "upickle" % "3.1.3",
      //"org.typelevel" %% "cats-effect" % "3.5.2",
      //"com.github.tototoshi" %% "scala-csv" % "1.4.1",
      "com.softwaremill.sttp.client3" %% "core" % "3.8.15"
    )
  )


