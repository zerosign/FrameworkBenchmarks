package http4s.techempower.benchmark.routes

import cats.effect.{
  ConcurrentEffect,
  ContextShift,
  Timer
}

import org.http4s.dsl.Http4sDsl
import http4s.techempower.benchmark.repo._

final class Routes[F[_]](repo: Repository[F])
  (implicit F: ConcurrentEffect[F], cs: ContextShift[F], timer: Timer[F]) extends Http4sDsl[F] {

  import fs2.Stream
  import io.circe.literal._
  import io.circe.syntax._
  import io.circe.generic.auto._
  import org.http4s.circe._
  import org.http4s.HttpRoutes

  import http4s.techempower.benchmark.model._

  object QueryLimit extends ValidatingQueryParamDecoderMatcher[Int]("queries")

  val helloWorldJson = json"""{ "message" : "Hello, World!" }"""

  val headerHtml : Stream[F, Byte] = Stream.emits("""<!DOCTYPE html>
  <html>
  <head><title>Fortunes</title></head>
  <body>
  <table>
      <tr><th>id</th><th>message</th></tr>""".getBytes("UTF-8"))

  val endHtml : Stream[F, Byte] = Stream.emits("""</table>
</body>
</html>""".getBytes("UTF-8"))

  @inline final def contentHtml(id: Int, message: String) : Stream[F, Byte] = ???
    // Array(
    //   "<tr></td>", id.toString, "</td><td>", message, "</td></tr>"
    // ).map(s => Stream.apply(s.getBytes("UTF-8"):_*)).fold(_ ++ _)

  // fetches Stream[F, Fortune] returns Stream[F, Byte]
  @inline def template(fortunes: Stream[F, Fortune]) : Stream[F, Byte] =
    headerHtml ++ fortunes.flatMap(f => contentHtml(f.id, f.message)) ++ endHtml

  val routes : HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => Ok("hello world")
    case GET -> Root / "plaintext" => Ok("Hello, World!")
    case GET -> Root / "json" => Ok(helloWorldJson)
    case GET -> Root / "fortunes" => Ok(
      template(
        Stream.evalSeq(
          repo.fetchFortunes()
          .append(Stream.apply(Fortune(0, "Additional fortune added at request time.")))
          .compile
          .toList
        )
      )
    )

    case GET -> Root / "db" => Ok(F.map(repo.fetchRandomWorld())(_.asJson))
    case GET -> Root / "queries" => Ok(repo.fetchRandomWorldUsingCond().take(1).map(_.asJson))
    case GET -> Root / "queries" :? QueryLimit(limit) =>
      limit.fold(
        errors => Ok(repo.fetchRandomWorldUsingCond().take(1).map(_.asJson)),
        num => Ok(repo.fetchRandomWorldUsingCond().take(Math.min(1, Math.max(500, num))).map(_.asJson))
      )

    case GET -> Root / "updates" => Ok(F.map(repo.updateRandomWorld())(_.asJson))
    case GET -> Root / "updates" :? QueryLimit(limit) =>
      limit.fold(
        errors => Ok(F.map(repo.updateRandomWorld())(_.asJson)),
        num => Ok(F.map(repo.updateRandomWorlds(Math.min(1, Math.max(500, num))))(_.asJson))
      )

  }
}
