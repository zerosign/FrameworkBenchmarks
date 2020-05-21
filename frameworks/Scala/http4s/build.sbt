import Dependencies._

ThisBuild / scalaVersion     := "2.13.2"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.techempower"
ThisBuild / organizationName := "techempower"

lazy val root = (project in file("."))
  .settings(
    name := "http4s",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      // Optional for auto-derivation of JSON codecs
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-literal" % circeVersion,
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-hikari" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    ),
    scalacOptions ++= Seq(
      "-target:11",
      "-Xverify",
      //"-Werror",
      "-verbose",
      "-opt:l:inline",
      "-Yopt-inline-heuristics:everything",
      "-opt-inline-from:**",
      //"-opt-warnings:_",
      "-deprecation"
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
