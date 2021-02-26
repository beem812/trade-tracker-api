package beem812.tradetracker

import scala.concurrent.ExecutionContext

import beem812.tradetracker.config.Config._
import cats.effect.Async
import cats.effect.Blocker
import cats.effect.ContextShift
import cats.effect.Resource
import cats.effect.Sync
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

object Database {
  def transactor[F[_]: Async](config: DatabaseConfig, executionContext: ExecutionContext)(implicit contextShift: ContextShift[F]): Resource[F, HikariTransactor[F]] = {
    for {
      blocker <- Blocker[F]
      xa <- HikariTransactor.newHikariTransactor[F](config.driver, config.url, config.user, config.password, executionContext, blocker)
    } yield xa
  }

  def initialize[F[_]: Sync](transactor: HikariTransactor[F]): F[Unit] = {
    transactor.configure{ dataSource => 
      Sync[F].delay{
        val flyway = Flyway.configure().dataSource(dataSource).load()
        flyway.migrate()
        ()
      }
    }
  }
}

