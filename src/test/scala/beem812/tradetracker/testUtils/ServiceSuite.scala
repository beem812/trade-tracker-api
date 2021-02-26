package beem812.tradetracker.testUtils

import munit.CatsEffectSuite
import beem812.tradetracker.config.Config._
import beem812.tradetracker.Database
import cats.effect.IO

class ServiceSuite extends CatsEffectSuite {
  import scala.concurrent.ExecutionContext.global
  val config = DatabaseConfig("org.h2.Driver", "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE", "sa", "", 16) 
  val transactor = Database.transactor[IO](config, global).map{ xa =>
    for {
      _ <- Database.initialize(xa)
    } yield xa
  }
}