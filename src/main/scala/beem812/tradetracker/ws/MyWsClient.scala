package beem812.tradetracker.ws

import org.http4s.client.jdkhttpclient._
import cats.effect._
import org.http4s.implicits._
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.websocket.WebSocketFrame
import beem812.tradetracker.ws.OutboundMessage._
import beem812.tradetracker.ws.UserMessage._
import cats.effect.concurrent.Ref
import beem812.tradetracker.algebras.LiveResearch
import cats.Monad

object MyWsClient {
  val finnHubUri = uri"wss://ws.finnhub.io?token=c1ah4pf48v6v5v4gu980"

  def make[F[_]: ConcurrentEffect: ContextShift]: Resource[F, Resource[F, WSConnectionHighLevel[F]]] = {
    val thing = JdkWSClient.simple[F].map(cli => cli.connectHighLevel(WSRequest(finnHubUri)))
    Resource.liftF(thing)
  } 

  def make2[F[_]: ConcurrentEffect: ContextShift](): Resource[F, WSConnectionHighLevel[F]] =
    Resource.liftF(JdkWSClient.simple[F])
    .flatMap{client => client.connectHighLevel(WSRequest(finnHubUri))}

  def make3[F[_]: ConcurrentEffect: ContextShift]: Resource[F, WSConnectionHighLevel[F]] = Resource.liftF(JdkWSClient.simple[F])
    .flatMap{ client => client.connectHighLevel(WSRequest(uri"wss://localhost:8080/receive"))}

  def reportResponses[F[_]: Sync: Concurrent](conn: WSConnectionHighLevel[F]): Resource[F, F[Unit]] = Concurrent[F].background {
    conn.receiveStream
      .collect{ case WSFrame.Text(data,_) => println(data)
                case WSFrame.Binary(data, _) => println(data)
                case _ => println("something else")}.compile.drain
  } 

  def reportOurResponses[F[_]: Sync: Concurrent](conn: WSConnectionHighLevel[F]): Resource[F, F[Unit]] = Concurrent[F].background {
    conn.receiveStream
      .collect{ case WSFrame.Text(data,_) => println("received from our socket: " + data.asJson.as[TickerSubscriptionMessage])
                case WSFrame.Binary(data, _) => println(data)
                case _ => println("something else")}.compile.drain
  } 

  def streamToClient[F[_]: Sync](finnhubConnection: WSConnectionHighLevel[F], subs: Ref[F, Set[String]]): fs2.Stream[F, WebSocketFrame.Text] = 
    finnhubConnection.receiveStream.collect{
      case WSFrame.Text(data, _) => parseMessage(data)
    }.evalMap{ message => for {
        subs <- subs.get
      } yield (subs, message)
      
    }.collect{
      case (subs, message: LatestPrice) if subs.contains(message.ticker) => WebSocketFrame.Text(message.toString())
    }
  
  def pipeFromClient[F[_]: Sync: Monad](finnhubConnection: WSConnectionHighLevel[F], subs: Ref[F, Set[String]]) =
    (wsfStream: fs2.Stream[F, WebSocketFrame]) => {
      wsfStream.collect{
        case WebSocketFrame.Text(txt, _) => UserMessage.parseUserMessage(txt)
      }.evalTap{
        case TickerSubscriptionMessage(message) => subs.modify((subs) => (subs + message.symbol, ()))
        case _ => ().pure[F]
      }.collect{
        case TickerSubscriptionMessage(message)=> {
          WSFrame.Text(message.toString())
        }
      }.evalMap(finnhubConnection.send(_))
  }
}