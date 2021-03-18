package beem812.tradetracker.algebras

import beem812.tradetracker.domain.Research.Price
import cats.effect.Sync
import beem812.tradetracker.domain.trade._
import beem812.tradetracker.domain.CreditDebit
import beem812.tradetracker.domain.Action

trait Analysis[F[_]] {
  def getSMA20(prices: List[Price]): F[Double]

  def getSMA50(prices: List[Price]): F[Double]

  def getRSI(prices: List[Price]): F[Double]

  def getWildersRSI(prices: List[Price]): F[Double]
}

object LiveAnalysis {
  def make[F[_]: Sync]() = Sync[F].delay(new LiveAnalysis[F])
}


final class LiveAnalysis[F[_]: Sync] extends Analysis[F] {

  def getSMA20(prices: List[Price]): F[Double] = Sync[F].delay{
    prices.take(20).foldLeft(0.0)(_ + _.close) / 20
  }

  def getSMA50(prices: List[Price]): F[Double] = Sync[F].delay{
    prices.take(50).foldLeft(0.0)(_ + _.close) / 50
  }

  def getRSI(prices: List[Price]): F[Double] = Sync[F].delay{
    val runUp = prices.drop(14).take(14)
    val (prevGains, prevLosses) = gainsAndLosses(runUp)
    // val last14 = prices.take(14)
    val List(stuff1, stuff2) = prices.drop(13).take(2)
    val firstaverage = if(stuff1.close - stuff2.close >= 0){
      ((prevGains + stuff1.close - stuff2.close) / 14, prevLosses / 14)
    } else {

      (prevGains / 14,(prevLosses + stuff2.close - stuff1.close ) / 14)
    }
    val last13Pairs = prices.take(13).map(_.close).reverse.sliding(2,1).toList 
    val (averageUp, averageDown) = last13Pairs.foldLeft(firstaverage){
      case ((prevUp, prevDown), List(prevClose, currClose)) => {
        val gainLoss = currClose - prevClose
        if(gainLoss >= 0){
          ((prevUp * 13 + gainLoss) / 14, prevDown * 13 / 14)
        } else {
          ((prevUp * 13) / 14, ((prevDown * 13) - gainLoss)/ 14)
        }
      }
      case ((prevUp, prevDown), _) => (prevUp, prevDown)
    }
    
    // val (gains, losses) = gainsAndLosses(last14) 
    val relativeStrength = averageUp / averageDown

    100 - (100 / (1 + relativeStrength))
  }

  def getWildersRSI(prices: List[Price]): F[Double] = Sync[F].delay{
    val previousSum = prices.drop(14).take(14).foldLeft(0.0)(_ + _.close)
    val previousAverage = previousSum / 14
    val (_, wsma) = prices.take(14).reverse.foldLeft((previousSum, previousAverage)){
      case ((prevSum, prevAv), price) =>  (prevSum - prevAv + price.close, (prevSum - prevAv + price.close) / 14)
    }

    100 - (100/ (1 + wsma))
  }

  def getThreeDayTrend(prices: List[Price]): F[Double] = Sync[F].delay{
    val today = prices.head.close
    val threeDays = prices.drop(2).head.close
    today - threeDays
  }

  def gainsAndLosses(prices: List[Price]): (Double, Double) = {
    val (gains, losses) = prices.map(_.close).sliding(2,1).toList.foldLeft((0.0, 0.0)){
      case ((gains, losses), List(first, second)) => if(first > second){ (gains + first - second, losses)} else {(gains, losses + second - first)}

      case ((gains, losses), _) => (gains, losses)

      case (_, _) => (0.0, 0.0)
    }

    (gains, losses)
  }

  def getCostBasis(trades: List[WheelTrade]) = Sync[F].delay {
    val totalCredit: Double = trades.map{
      case WheelTrade(_, _, _, _, price, shares, CreditDebit.Credit) => price.toDouble * shares * 1.0D
      case WheelTrade(_, _, _, _, price, shares, CreditDebit.Debit) => price.toDouble * shares * -1.0D
      case _ => 0.0D
    }.foldLeft(0.0D)(_ + _)

    val totalShares = trades.filter((trade: WheelTrade) => trade.action == Action.SharesAssigned || trade.action == Action.SharesCalled).foldLeft(0.0){
      case(acc, WheelTrade(_, _, Action.SharesAssigned, _, _, shares, _)) => acc + shares
      case (acc, WheelTrade(_, _, Action.SharesCalled, _, _, shares, _)) => acc - shares
      case (acc, _) => acc 
    }

    CostBasisData(totalCredit, totalShares, Math.abs(totalCredit / totalShares))
  }


}