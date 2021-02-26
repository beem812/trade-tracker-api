package beem812.tradetracker

import beem812.tradetracker.algebras._
import beem812.tradetracker.http.Client
import cats.effect._
import cats.effect.laws.util.TestContext
import munit.CatsEffectSuite
import beem812.tradetracker.algebras.LiveAnalysis
import scala.concurrent.ExecutionContext.global
import beem812.tradetracker.config.Config.DatabaseConfig
import beem812.tradetracker.programs._
import beem812.tradetracker.domain.trade.WheelTrade
import beem812.tradetracker.domain._

class HelloWorldSpec2 extends CatsEffectSuite {
  case class User(userId: Int, id: Int, title: String, completed: Boolean)
  implicit val ec = TestContext()

  val config = DatabaseConfig("org.h2.Driver", "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE", "sa", "", 16) 

  val transactor = Database.transactor[IO](config, global)

  val service = Client.make[IO]().use{ httpClient => 
    for {
        analysis <- LiveAnalysis.make[IO]()
        research <- LiveResearch.make[IO](httpClient)
        screener <- LiveStockScreener.make[IO](research, analysis)
    } yield screener
  }

  val tracker = ResourceFixture( transactor.map{ xa => 
    for {
      _ <- Database.initialize[IO](xa)
      tradeAlg <- LiveTrades.make[IO]()
      tradeProg <- LiveTradeTracker.make[IO](tradeAlg, xa)
    } yield tradeProg
  })

  tracker.test("insert"){ s =>
    val newTrade = WheelTrade(None, "CRSR", Action.CC, "", 3.90, 100, CreditDebit.Credit)
    val result = for { 
      t <- s
      id <- t.insertTrade(newTrade)
      trades <- t.getTrades()
    } yield (trades.exists(t => t.id == id), trades.length)
    assertIO(result, (true, 2))
  }

    tracker.test("insert2"){ s =>
    val newTrade = WheelTrade(None, "CRSR", Action.CC, "", 3.90, 100, CreditDebit.Credit)
    val result = for { 
      t <- s
      id <- t.insertTrade(newTrade)
      trades <- t.getTrades()
    } yield (trades.exists(t => t.id == id), trades.length)
    assertIO(result, (true, 2))
  }
}