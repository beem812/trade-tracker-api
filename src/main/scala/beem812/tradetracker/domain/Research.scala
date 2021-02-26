package beem812.tradetracker.domain

import io.circe.Json
import io.circe.Decoder
import io.circe.HCursor
import io.circe.generic.auto._

object Research {
  implicit val decodePriceHistory: Decoder[PriceHistory] = new Decoder[PriceHistory] {
    final def apply(c: HCursor): Decoder.Result[PriceHistory] = 
    for {
      priceJson <- c.downField("prices").as[List[Json]]
      firstTradeDate <- c.downField("firstTradeDate").as[Int]
      id <- c.downField("id").as[String]
      isPending <- c.downField("isPending").as[Boolean]

    } yield PriceHistory("", parsePrices(priceJson), firstTradeDate, id, isPending)
  }

  def parsePrices(pricesJson: List[Json]) = {
    pricesJson.map{ price => 
      price.as[Price]
    }.collect{
      case Right(value) => value
    }
  }  

  case class Price(date: Int, open: Double, high: Double, low: Double,  close: Double,   volume: Int, adjclose: Double)
  case class PriceHistory(symbol: String, prices: List[Price], firstTradeDate: Int, id: String, isPending: Boolean)
  case class IntermediatePriceHistory(prices: List[Json], firstTradeDate: Int, id: String, isPending: Boolean)
}