package beem812.tradetracker.programs

import beem812.tradetracker.algebras.LiveTradesRepo
import cats.effect.IO
import beem812.tradetracker.testUtils._
import beem812.tradetracker.domain._
import beem812.tradetracker.domain.trade._
import beem812.tradetracker.algebras.LiveAnalysis
import cats.implicits._


class TradeTrackerSpec extends ServiceSuite {
  val trackerProgram = ResourceFixture(transactor.map{xa => 
      for {
        trans <- xa
        analysisAlg = LiveAnalysis.make[IO]()
        alg = LiveTradesRepo.make[IO]()
        service = LiveTradeTracker.make[IO](alg, analysisAlg, trans)
      } yield service
  })

  trackerProgram.test("insert"){ s =>
    val newTrade = WheelTrade(None, "CRSR", Action.CC, "", 3.90, 100, CreditDebit.Credit)
    val result = for { 
      t <- s
      id <- t.insertTrade(newTrade)
      trades <- t.getTrades()
    } yield trades.find(t => t.id == id).get.copy(id = None)
  
    result.assertEquals(newTrade)
  }

  trackerProgram.test("cost basis"){service => 
    val trades = List(
      WheelTrade(None, "PLTR", Action.CSP, "", 3.60, 100, CreditDebit.Credit),
      WheelTrade(None, "PLTR", Action.CSP, "", 2.82, 100, CreditDebit.Debit),
      WheelTrade(None, "PLTR", Action.CSP, "", 4.95, 100, CreditDebit.Credit),
      WheelTrade(None, "PLTR", Action.CSP, "", 4.60, 100, CreditDebit.Debit),
      WheelTrade(None, "PLTR", Action.CSP, "", 3.95, 100, CreditDebit.Credit),
      WheelTrade(None, "PLTR", Action.CSP, "", 3.65, 100, CreditDebit.Credit),
      WheelTrade(None, "PLTR", Action.SharesAssigned, "", 29, 200, CreditDebit.Debit),
    )

    val result = for {
      tracker <- service
      _ <- trades.map(tracker.insertTrade(_)).parSequence
      costBasis <- tracker.getCostBasis("PLTR")
    } yield costBasis
    assertIO(result, CostBasisData(-4927.0, 200.0,24.635)) 
  
  }
}