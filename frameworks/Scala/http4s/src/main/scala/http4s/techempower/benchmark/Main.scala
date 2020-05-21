package http4s.techempower.benchmark

import cats.effect.IOApp

object Main extends IOApp {
  import scala.concurrent.ExecutionContext
  import fs2.Stream
  import cats.effect.{ IO, Resource, ExitCode, Blocker }
  import doobie.hikari.HikariTransactor
  import doobie.util.{ ExecutionContexts => DoobieEC }
  import org.http4s.server.blaze.BlazeServerBuilder
  import org.http4s.server.Router
  import org.http4s.HttpRoutes
  import routes._
  import repo._

  /// pass database to routes
  @inline final def openDatabase(
    host: String,
    poolSize: Int
  ): Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- DoobieEC.fixedThreadPool[IO](32) // our connect EC
      be <- Blocker[IO] // our blocking EC
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        s"jdbc:postgresql://$host/hello_world",
        "benchmarkdbuser",
        "benchmarkdbpass",
        ce,
        be
      )
      _ <- Resource.liftF(
        xa.configure(
          ds =>
            IO {
              ds.setMaximumPoolSize(poolSize)
              ds.setMinimumIdle(poolSize)
            }
        )
      )
    } yield xa

  import org.http4s.implicits._

  @inline final def run(args: List[String]) : IO[ExitCode] = {
    val dbHost = args.headOption.getOrElse("localhost")
    val dbPoolSize = sys.env.get("DB_POOL_SIZE").map(_.toInt).getOrElse(256)
    val db = Stream.resource(openDatabase(dbHost, dbPoolSize))
    val repository : Repository[IO] = new Repository(db)
    val service = new Routes(repository)
    val routes : HttpRoutes[IO] = service.routes
    val app = Router("/" -> routes).orNotFound

    BlazeServerBuilder[IO](ExecutionContext.global)
      .bindHttp(8080, "0.0.0.0")
      .withoutBanner
      .withHttpApp(app)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}
