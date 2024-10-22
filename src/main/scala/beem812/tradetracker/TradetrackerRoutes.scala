package beem812.tradetracker

import beem812.tradetracker.algebras.LiveResearch
import beem812.tradetracker.domain.trade._
import beem812.tradetracker.programs.TradeTracker
import beem812.tradetracker.ws.UserMessage._
import beem812.tradetracker.ws.OutboundMessage._
import cats.effect.Sync
import cats.implicits._
import fs2.concurrent.Queue
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import cats.effect.concurrent.Ref
import org.http4s.client.jdkhttpclient.WSFrame.Text
import io.circe.syntax._
import fs2.Chunk
import org.http4s.client.jdkhttpclient._
import beem812.tradetracker.algebras.LiveTickerSockets
import beem812.tradetracker.ws.MyWsClient
import cats.effect.Concurrent
import org.http4s.headers.Authorization

object TradetrackerRoutes {

  def jokeRoutes[F[_]: Sync](J: Jokes[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "joke" =>
        for {
          joke <- J.get
          resp <- Ok(joke)
        } yield resp
    }
  }

  def helloWorldRoutes[F[_]: Sync](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        for {
          greeting <- H.hello(HelloWorld.Name(name))
          resp <- Ok(greeting)
        } yield resp
    }
  }

  /**
   * Defines the HTTP routes for trade-related operations.
   *
   * @param T The TradeTracker instance for handling trade operations.
   * @param R The LiveResearch instance for handling research operations.
   * @param WS The WebSocket connection for handling real-time updates.
   * @return The HttpRoutes for trade-related operations.
   */
  def tradeRoutes[F[_]: Sync: Concurrent](T: TradeTracker[F], R: LiveResearch[F], WS: WSConnectionHighLevel[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      /**
       * Retrieves all trades.
       *
       * @return The list of trades.
       */
      case GET -> Root / "trades" =>
        T.getTrades().flatMap(Ok(_)) 
      
      /**
       * Retrieves the cost basis for a given ticker.
       *
       * @param ticker The stock ticker symbol.
       * @return The cost basis data.
       */
      case GET -> Root / ticker / "costbasis" =>
        T.getCostBasis(ticker).flatMap(Ok(_))

      /**
       * Inserts a new trade.
       *
       * @param req The HTTP request containing the trade data.
       * @return The ID of the inserted trade.
       */
      case req @ POST -> Root / "trade" =>
        req.as[WheelTrade]
          .flatMap(T.insertTrade)
          .flatMap(Ok(_))

      /**
       * Subscribes to ticker updates for a given symbol.
       *
       * @param ticker The stock ticker symbol.
       * @return Unit
       */
      case GET -> Root / "subscribe" / ticker => 
        R.subscribeToTicker(ticker.toUpperCase()).flatMap(Ok(_))

      /**
       * Handles WebSocket connections for receiving real-time updates.
       *
       * @return The WebSocket connection.
       */
      case GET -> Root / "receive"  => 
        val tickerSubs = Ref.of[F, Set[String]](Set.empty)

        for {
          ref <- tickerSubs
          streamedQuotes = MyWsClient.streamToClient(WS, ref)
          fromClient = MyWsClient.pipeFromClient(WS, ref)
          polledQuotes = R.streamQuotes(ref).map(q => WebSocketFrame.Text(LatestPrice(q.ticker, q.c).toString()))
          finalToClientStream = streamedQuotes.merge(polledQuotes)
          socket <- WebSocketBuilder[F].build(finalToClientStream, fromClient)
        } yield socket
      
      /**
       * Tries to authenticate the request using the provided authorization token.
       *
       * @param req The HTTP request containing the authorization token.
       * @return The result of the authentication attempt.
       */
      case req @ GET -> Root / "try-auth" =>
        val authToken = req.headers.get(Authorization).toRight("Couldn't find the auth header")
        println(authToken)
        Ok()
    }
  }
}
