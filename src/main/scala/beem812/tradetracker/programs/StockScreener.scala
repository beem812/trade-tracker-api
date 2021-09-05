package beem812.tradetracker.programs

import beem812.tradetracker.algebras.Research
import beem812.tradetracker.domain.Research.PriceHistory
import cats.effect.Sync
import beem812.tradetracker.algebras.Analysis
import cats.Parallel
import cats.implicits._

trait StockScreener[F[_]] {
  def getTwentyDaySMA(symbol: String): F[(Double, Double)]
}

object LiveStockScreener {
  def make[F[_]: Parallel: Sync](researchClient: Research[F], analysis: Analysis[F]) = new LiveStockScreener[F](researchClient, analysis)
}

final class LiveStockScreener[F[_]: Parallel: Sync] private (val researchClient: Research[F], val analysis: Analysis[F]) extends StockScreener[F] {
  def getTwentyDaySMA(symbol: String): F[(Double, Double)] = {

    

    for {
      PriceHistory(symbol, prices, _,_,_) <- researchClient.getHistoricalData(symbol)
      (rsi, wrsi, sma20, sma50) <- (analysis.getRSI(prices), analysis.getWildersRSI(prices), analysis.getSMA20(prices), analysis.getSMA50(prices)).parMapN((_,_,_,_))
    } yield (rsi, wrsi) 

  }

}

/**
  * price over 50
  * underlying 3 day trend negative
  * underlying price relative to SMA20  +/- 5%
  * underlying Price relative to SMA50: Above SMA50
  * RSI: < 70  not oversold 
  * atm implied volatility > 50%
  * options volume: > 30000
  */
