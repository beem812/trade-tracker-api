package beem812.tradetracker.algebras

import doobie.quill.DoobieContext
import cats.effect.Sync
import doobie._
import beem812.tradetracker.domain.trade.WheelTrade


trait TradesRepo {
  def getTrades(): ConnectionIO[List[WheelTrade]]

  def tradesByTicker(ticker: String): ConnectionIO[List[WheelTrade]]
  
  def insertTrade(trade: WheelTrade): ConnectionIO[Option[String]]
}

object LiveTradesRepo {
  def make[F[_]: Sync](): LiveTradesRepo = 
    new LiveTradesRepo()
}

final class LiveTradesRepo  extends TradesRepo {
  val ctx = new DoobieContext.MySQL[io.getquill.SnakeCase](io.getquill.SnakeCase)

  import ctx._

  def getTrades(): ConnectionIO[List[WheelTrade]] = {
    val q = quote{ query[WheelTrade]}
    run(q)
  }

  def tradesByTicker(ticker: String): ConnectionIO[List[WheelTrade]] = {
    val q = quote{ query[WheelTrade].filter{(trade) => trade.ticker == lift(ticker)}}
    run(q)
  }

  def insertTrade(trade: WheelTrade) = {
    val q = quote { query[WheelTrade].insert(lift(trade)).returningGenerated(_.id)} 
    run(q)
  }

}