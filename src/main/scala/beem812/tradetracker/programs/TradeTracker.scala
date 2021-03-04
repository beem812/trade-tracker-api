package beem812.tradetracker.programs

import beem812.tradetracker.algebras.TradesRepo
import doobie.implicits._
import cats.effect.Sync
import beem812.tradetracker.domain.trade.WheelTrade
import doobie.util.transactor.Transactor
import beem812.tradetracker.algebras.LiveAnalysis
import cats.implicits._

trait TradeTracker[F[_]] {
  def getTrades(): F[List[WheelTrade]]

  def insertTrade(trade: WheelTrade): F[Option[String]]
}

object LiveTradeTracker {
  def make[F[_]: Sync](trades: TradesRepo, 
   analysis: LiveAnalysis[F], 
  transactor: Transactor[F])= {
    Sync[F].delay{
      new LiveTradeTracker(trades, analysis, transactor)
    }
  }
}

final class LiveTradeTracker[F[_]: Sync] (
  trades: TradesRepo,
  analysis: LiveAnalysis[F],
  transactor: Transactor[F]
) extends TradeTracker[F]{


  def getTrades() = {
    val thing = trades.getTrades()
    thing.transact(transactor) 
  }

  def insertTrade(trade: WheelTrade): F[Option[String]] = {
    trades.insertTrade(trade).transact(transactor)
  }

  def getCostBasis(ticker: String) = {
    for {
      t <- trades.tradesByTicker(ticker).transact(transactor)
      basis <- analysis.getCostBasis(t)
    } yield basis
  }
}