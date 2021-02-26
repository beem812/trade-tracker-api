package beem812.tradetracker.algebras

import beem812.tradetracker.domain.Research._
import cats.effect.Sync
import org.http4s.Header
import org.http4s.Headers
import org.http4s.HttpVersion
import org.http4s.Method
import org.http4s.Request
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.implicits._
import io.scalaland.chimney.dsl._
import cats.syntax.all._

trait Research[F[_]] {
  def getHistoricalData(ticker: String): F[PriceHistory]
}

object LiveResearch {
  def make[F[_]: Sync](client: Client[F]): F[LiveResearch[F]] = 
    Sync[F].delay{
      new LiveResearch[F](client)
    }
}

final class LiveResearch[F[_]: Sync] private (client: Client[F]) extends Research[F] {
  def searchTickerGoogle(ticker: String): F[String] = ???

  def getHistoricalData(symbol: String): F[PriceHistory] = {
    val rapiApiUri = uri"https://apidojo-yahoo-finance-v1.p.rapidapi.com/stock/v3/get-historical-data"+?("symbol", symbol)

    val req = Request[F](Method.GET, rapiApiUri, HttpVersion.`HTTP/1.1`, Headers(List(Header("X-RapidAPI-Key", "3cf4a5d1cfmshe4c28305a3191ddp1ac837jsn376d5ca81a44"))))

    client.expect[PriceHistory](req).map(_.into[PriceHistory].withFieldConst(_.symbol, symbol).transform)
  }

  def searchTickerBarChartCom(ticker: String): F[String] = ???
}