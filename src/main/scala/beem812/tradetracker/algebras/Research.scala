package beem812.tradetracker.algebras

import beem812.tradetracker.domain.Research._
import cats.effect.Sync
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.implicits._
import io.scalaland.chimney.dsl._
import cats.syntax.all._
import org.http4s.client.jdkhttpclient._
import io.circe.syntax._
import io.circe.generic.auto._
import scala.concurrent.duration._
import cats.effect.Async
import cats.effect.Timer
import cats.effect.ConcurrentEffect
import cats.effect.concurrent.Ref

/**
 * The Research trait provides methods for retrieving historical stock data,
 * subscribing to ticker updates, and getting stock quotes.
 */
trait Research[F[_]] {
  def getHistoricalData(ticker: String): F[PriceHistory]
  def subscribeToTicker(ticker: String): F[Unit] 
  def getStockQuote(ticker: String): F[StockQuoteWithTicker]
}

object LiveResearch {
  case class TickerSubscriptionMessage(`type`: String, symbol: String)

  def make[F[_]: Sync: Timer](client: Client[F], ws: WSConnectionHighLevel[F]) = 
    new LiveResearch[F](client, ws)
}

/**
 * The LiveResearch class implements the Research trait and provides concrete
 * implementations for retrieving historical stock data, subscribing to ticker
 * updates, and getting stock quotes.
 */
final class LiveResearch[F[_]: Sync: Timer] private (client: Client[F], ws: WSConnectionHighLevel[F]) extends Research[F] {
  import LiveResearch._
  def searchTickerGoogle(ticker: String): F[String] = ???

  /**
   * Retrieves historical stock data for a given symbol.
   *
   * @param symbol The stock symbol.
   * @return The historical price data.
   */
  def getHistoricalData(symbol: String): F[PriceHistory] = {
    val rapiApiUri = uri"https://apidojo-yahoo-finance-v1.p.rapidapi.com/stock/v3/get-historical-data"+?("symbol", symbol)

    val req = Request[F](Method.GET, rapiApiUri, HttpVersion.`HTTP/1.1`, Headers(List(Header("X-RapidAPI-Key", "3cf4a5d1cfmshe4c28305a3191ddp1ac837jsn376d5ca81a44"))))

    client.expect[PriceHistory](req).map(_.into[PriceHistory].withFieldConst(_.symbol, symbol).transform)
  }

  def searchTickerBarChartCom(ticker: String): F[String] = ???

  /**
   * Subscribes to ticker updates for a given symbol.
   *
   * @param ticker The stock ticker symbol.
   * @return Unit
   */
  def subscribeToTicker(ticker: String): F[Unit] = for{
    _ <- Sync[F].delay(println("subscribing"))
    _ <- ws.send(WSFrame.Text(TickerSubscriptionMessage("subscribe", ticker).asJson.toString()))
  } yield ()

  /**
   * Retrieves the current stock quote for a given ticker.
   *
   * @param ticker The stock ticker symbol.
   * @return The stock quote with ticker information.
   */
  def getStockQuote(ticker: String): F[StockQuoteWithTicker] = {
    val finnhubUri =uri"https://finnhub.io/api/v1/quote?&token=c1ah4pf48v6v5v4gu980"+?("symbol", ticker)
    val req = Request[F](Method.GET, finnhubUri, HttpVersion.`HTTP/1.1`)
    client.expect[StockQuote](req).map(_.into[StockQuoteWithTicker].withFieldConst(_.ticker, ticker).transform)
  }
  
  def streamQuotes(tickers: Ref[F, Set[String]]): fs2.Stream[F, StockQuoteWithTicker] = {
    fs2.Stream.awakeEvery[F](15.second).evalMap{ _ =>
      for{
        currentTickers <- tickers.get
        quotes <- currentTickers.toList.traverse(getStockQuote(_))
      }yield quotes
    }.flatMap(fs2.Stream.emits)
  }
  
}
