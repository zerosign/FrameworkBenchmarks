package http4s.techempower.benchmark.repo

import cats.effect.{Resource, ConcurrentEffect}
import fs2.Stream
import doobie.Transactor

final class Repository[F[_]](r: Stream[F, Transactor[F]])(implicit F: ConcurrentEffect[F]) {
  import java.util.concurrent.ThreadLocalRandom
  import http4s.techempower.benchmark.model._
  import doobie.implicits._

  val randomId : F[Int] =  F.delay {
    ThreadLocalRandom.current.nextInt(1, 10001)
  }

  val randomIds : Stream[F, Int] = Stream.eval(randomId).repeat

  @inline final def fetchFortunes() :  Stream[F, Fortune] = r.flatMap { xa =>
      sql"select id, message from Fortune".query[Fortune]
        .stream.transact(xa)
  }

  @inline final def fetchWorldById(id: Int) : Stream[F, World] =
    r.flatMap { xa =>
      sql"select distinct(id, randomNumber) from World where id = ${id}".query[World]
        .stream.transact(xa)
    }

  @inline final def fetchRandomWorld() : F[Option[World]] =
    F.flatMap(randomId)(fetchWorldById(_).head.compile.last)

  @inline final def fetchWorldsByIds(ids: Stream[F, Int]) : Stream[F, World] =
    r.flatMap { xa =>
      Stream.force {
        F.map(ids.compile.toList) { ids =>
          val targets = ids.mkString(",")
          Stream.evalSeq(
            sql"select distinct(id, randomNumber) from World where id in ($targets)".query[World]
              .to[List]
              .transact(xa)
          )
        }
      }
    }

  @inline final def fetchRandomWorldUsingIn() : Stream[F, World] =
    fetchWorldsByIds(randomIds)

  @inline final def fetchRandomWorldUsingCond() : Stream[F, World] =
    randomIds.flatMap(fetchWorldById(_))

  @inline final def updateWorld(world: World, updatedNumber: F[Int]) : F[Int] =
    r.evalMap { xa =>
      sql"update World where id = ${world.id} set randomNumber = ${world.randomNumber}"
        .update
        .run
        .transact(xa)
    }.compile.lastOrError


  @inline final def updateWorlds(worlds: Stream[F, World]) : F[Int] =
    worlds.flatMap { world =>
      r.evalMap { xa =>
        sql"update World where id = ${world.id} set randomNumber = ${world.randomNumber}"
          .update
          .run
          .transact(xa)
      }
    }.compile.fold(0)((r, _) => r + 1)

  @inline final def updateRandomWorld() : F[Int] =
    F.flatMap(fetchRandomWorld()) {
        case Some(world) => updateWorld(world, randomId)
        case _ => F.delay(0)
    }

  @inline final def updateRandomWorlds(limit: Int) : F[Int] =
    updateWorlds(fetchRandomWorldUsingCond().take(limit))
}
