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

  def tradeRoutes[F[_]: Sync: Concurrent](T: TradeTracker[F], R: LiveResearch[F], WS: WSConnectionHighLevel[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "trades" =>
        T.getTrades().flatMap(Ok(_)) 
      
      case GET -> Root / ticker / "costbasis" =>
        T.getCostBasis(ticker).flatMap(Ok(_))

      case req @ POST -> Root / "trade" =>
        req.as[WheelTrade]
          .flatMap(T.insertTrade)
          .flatMap(Ok(_))


      case GET -> Root / "subscribe" / ticker => 
        R.subscribeToTicker(ticker.toUpperCase()).flatMap(Ok(_))

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
      
      case req @ GET -> Root / "try-auth" =>
        val authToken = req.headers.get(Authorization).toRight("Couldn't find the auth header")
        println(authToken)
        Ok()
    }
  }
}