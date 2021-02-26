package beem812.tradetracker.programs

import beem812.tradetracker.algebras.Trades
import doobie.implicits._
import cats.effect.Sync
import beem812.tradetracker.domain.trade.WheelTrade
import doobie.util.transactor.Transactor

trait TradeTracker[F[_]] {
  def getTrades(): F[List[WheelTrade]]

  def insertTrade(trade: WheelTrade): F[Option[String]]
}

object LiveTradeTracker {
  def make[F[_]: Sync](trades: Trades, transactor: Transactor[F])= {
    Sync[F].delay{
      new LiveTradeTracker(trades, transactor)
    }
  }
}

final class LiveTradeTracker[F[_]: Sync] (
  trades: Trades,
  transactor: Transactor[F]
) extends TradeTracker[F]{


  def getTrades() = {
    val thing = trades.getTrades()
    thing.transact(transactor) 
  }

  def insertTrade(trade: WheelTrade): F[Option[String]] = {
    trades.insertTrade(trade).transact(transactor)
  }
}