package beem812.tradetracker

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import beem812.tradetracker.programs.TradeTracker
import beem812.tradetracker.domain.trade._
import io.circe.generic.auto._
// import beem812.tradetracker.domain.Action
// import beem812.tradetracker.domain.CreditDebit


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

  def tradeRoutes[F[_]: Sync](T: TradeTracker[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "trades" =>
        T.getTrades().flatMap(Ok(_)) 
      
      case GET -> Root / ticker / "costbasis" =>
        T.getCostBasis(ticker).flatMap(Ok(_))

      case req @ POST -> Root / "trade" => {
        req.as[WheelTrade]
          .flatMap(T.insertTrade(_))
          .flatMap(Ok(_))
      }
    }
  }
}