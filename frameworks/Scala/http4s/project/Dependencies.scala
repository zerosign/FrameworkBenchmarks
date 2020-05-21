import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.3.0-SNAP2"
  lazy val http4sVersion = "0.21.4"
  lazy val doobieVersion = "0.9.0"
  lazy val circeVersion = "0.13.0"

  lazy val inlineExceptions : Seq[String] = Seq(
    "cats.effect.Blocker",
    "java.lang.StringBuilder",
    "http4s.techempower.benchmark.routes.Routes",
    "scala.concurrent.ExecutionContext",
    "java.lang.Integer",
    "scala.Predef$",
    "scala.reflect.ClassTag",
    "org.http4s.server.package$defaults$",
    "scala.collection.SeqFactory$Delegate",
    "java.util.concurrent.ThreadLocalRandom"
  )

  lazy val inlineExceptionsFormat : String =
    inlineExceptions.map(s => s"!${s}").mkString(",")
}
