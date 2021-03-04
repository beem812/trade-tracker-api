package beem812.tradetracker.programs

import beem812.tradetracker.algebras.LiveTrades
import cats.effect.IO
import beem812.tradetracker.testUtils._
import beem812.tradetracker.domain._


class TradeTrackerSpec extends ServiceSuite {
  val trackerProgram = ResourceFixture(transactor.map{xa => 
      for {
        trans <- xa
        alg <- LiveTrades.make[IO]()
        service <- LiveTradeTracker.make[IO](alg, trans)
      } yield service
  })

  trackerProgram.test("insert"){ s =>
    val newTrade = trade.WheelTrade(None, "PLTR", Action.CC, "", 3.90, 100, CreditDebit.Credit)
    val result = for { 
      t <- s
      id <- t.insertTrade(newTrade)
      trades <- t.getTrades()
    } yield trades.find(t => t.id == id).get.copy(id = None)
  
    result.assertEquals(newTrade)
  }
}