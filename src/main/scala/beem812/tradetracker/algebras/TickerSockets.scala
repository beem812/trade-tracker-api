package beem812.tradetracker.algebras

import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.client.jdkhttpclient._
import cats._

trait TickerSockets[F[_]] {
  def subscribeToTicker(string: String): F[Unit]
}

object LiveTickerSockets {
  case class TickerSubscriptionMessage(`type`: String, symbol: String)
  def make[F[_]: Monad](ws: WSConnectionHighLevel[F]) = new LiveTickerSockets[F](ws)
}

final class LiveTickerSockets[F[_]:Monad] private( tickerSocket: WSConnectionHighLevel[F]) extends TickerSockets[F]{
  import LiveTickerSockets._
  def subscribeToTicker(ticker: String): F[Unit] = for{
    _ <- tickerSocket.send(WSFrame.Text(TickerSubscriptionMessage("subscribe", ticker).asJson.toString()))
  } yield ()   
}