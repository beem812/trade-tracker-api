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

  trackerProgram.test("getRSI"){ service =>
    val prices = List(
      Price(1, 10, 12, 8, 11, 1000, 11),
      Price(2, 11, 13, 9, 12, 1100, 12),
      Price(3, 12, 14, 10, 13, 1200, 13),
      Price(4, 13, 15, 11, 14, 1300, 14),
      Price(5, 14, 16, 12, 15, 1400, 15),
      Price(6, 15, 17, 13, 16, 1500, 16),
      Price(7, 16, 18, 14, 17, 1600, 17),
      Price(8, 17, 19, 15, 18, 1700, 18),
      Price(9, 18, 20, 16, 19, 1800, 19),
      Price(10, 19, 21, 17, 20, 1900, 20),
      Price(11, 20, 22, 18, 21, 2000, 21),
      Price(12, 21, 23, 19, 22, 2100, 22),
      Price(13, 22, 24, 20, 23, 2200, 23),
      Price(14, 23, 25, 21, 24, 2300, 24),
      Price(15, 24, 26, 22, 25, 2400, 25),
      Price(16, 25, 27, 23, 26, 2500, 26),
      Price(17, 26, 28, 24, 27, 2600, 27),
      Price(18, 27, 29, 25, 28, 2700, 28),
      Price(19, 28, 30, 26, 29, 2800, 29),
      Price(20, 29, 31, 27, 30, 2900, 30)
    )

    val result = for {
      tracker <- service
      rsi <- tracker.analysis.getRSI(prices)
    } yield rsi

    assertIO(result, 100.0)
  }

  trackerProgram.test("getWildersRSI"){ service =>
    val prices = List(
      Price(1, 10, 12, 8, 11, 1000, 11),
      Price(2, 11, 13, 9, 12, 1100, 12),
      Price(3, 12, 14, 10, 13, 1200, 13),
      Price(4, 13, 15, 11, 14, 1300, 14),
      Price(5, 14, 16, 12, 15, 1400, 15),
      Price(6, 15, 17, 13, 16, 1500, 16),
      Price(7, 16, 18, 14, 17, 1600, 17),
      Price(8, 17, 19, 15, 18, 1700, 18),
      Price(9, 18, 20, 16, 19, 1800, 19),
      Price(10, 19, 21, 17, 20, 1900, 20),
      Price(11, 20, 22, 18, 21, 2000, 21),
      Price(12, 21, 23, 19, 22, 2100, 22),
      Price(13, 22, 24, 20, 23, 2200, 23),
      Price(14, 23, 25, 21, 24, 2300, 24),
      Price(15, 24, 26, 22, 25, 2400, 25),
      Price(16, 25, 27, 23, 26, 2500, 26),
      Price(17, 26, 28, 24, 27, 2600, 27),
      Price(18, 27, 29, 25, 28, 2700, 28),
      Price(19, 28, 30, 26, 29, 2800, 29),
      Price(20, 29, 31, 27, 30, 2900, 30)
    )

    val result = for {
      tracker <- service
      wildersRsi <- tracker.analysis.getWildersRSI(prices)
    } yield wildersRsi

    assertIO(result, 100.0)
  }

  trackerProgram.test("gainsAndLosses"){ service =>
    val prices = List(
      Price(1, 10, 12, 8, 11, 1000, 11),
      Price(2, 11, 13, 9, 12, 1100, 12),
      Price(3, 12, 14, 10, 13, 1200, 13),
      Price(4, 13, 15, 11, 14, 1300, 14),
      Price(5, 14, 16, 12, 15, 1400, 15),
      Price(6, 15, 17, 13, 16, 1500, 16),
      Price(7, 16, 18, 14, 17, 1600, 17),
      Price(8, 17, 19, 15, 18, 1700, 18),
      Price(9, 18, 20, 16, 19, 1800, 19),
      Price(10, 19, 21, 17, 20, 1900, 20)
    )

    val result = for {
      tracker <- service
      gainsAndLosses <- tracker.analysis.gainsAndLosses(prices)
    } yield gainsAndLosses

    assertIO(result, (10.0, 0.0))
  }
}
