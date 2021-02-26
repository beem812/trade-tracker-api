package beem812.tradetracker.algebras

import beem812.tradetracker.domain.trade.Trade
// import doobie.hikari.HikariTransactor
import doobie.quill.DoobieContext
// import doobie.implicits._
import cats.effect.Sync
// import apertures.domain.sharedLink.CreateSharedLink
// import cats.implicits._
import doobie._
import beem812.tradetracker.domain.trade.WheelTrade


trait Trades {
  def getTrades(): ConnectionIO[List[WheelTrade]]
  
  def insertTrade(trade: WheelTrade): ConnectionIO[Option[String]]
}

object LiveTrades {
  def make[F[_]: Sync](): F[LiveTrades] = 
    Sync[F].delay{
      new LiveTrades()
    }
}

final class LiveTrades  extends Trades {
  val ctx = new DoobieContext.MySQL[io.getquill.SnakeCase](io.getquill.SnakeCase)

  import ctx._

  def getTrades(): ConnectionIO[List[WheelTrade]] = {
    val q = quote{ query[WheelTrade]}
    run(q)
  }

  def getTicker(ticker: String): ConnectionIO[List[Trade]] = {
    val q = quote{ query[Trade].filter{(trade) => trade.ticker == lift(ticker)}}
    run(q)
  }

  def insertTrade(trade: WheelTrade) = {
    val q = quote { query[WheelTrade].insert(lift(trade)).returningGenerated(_.id)} 
    run(q)
  }

}