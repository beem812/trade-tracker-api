package beem812.tradetracker

import scala.concurrent.ExecutionContext.global

import beem812.tradetracker.algebras.LiveAnalysis
import beem812.tradetracker.algebras.LiveTradesRepo
import beem812.tradetracker.config.Config
import beem812.tradetracker.programs.LiveTradeTracker
import cats.effect._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware._
import beem812.tradetracker.http.Client
import beem812.tradetracker.ws.MyWsClient
import beem812.tradetracker.algebras.LiveResearch
import fs2.concurrent.Queue
import beem812.tradetracker.ws.UserMessage
import cats.effect.concurrent.Ref

object Main extends IOApp {

  def server = for {
    config <- Config.load[IO]()
    transactor <- Database.transactor[IO](config.dbConfig, global)
    httpClient <- Client.make[IO]()
    wsConn <- MyWsClient.make2[IO]()
    researchAlg = LiveResearch.make[IO](httpClient, wsConn)
    tradeAlg = LiveTradesRepo.make[IO]()
    analysisAlg = LiveAnalysis.make[IO]()
    tradeProg = LiveTradeTracker.make[IO](tradeAlg, analysisAlg, transactor)
    httpApp = TradetrackerRoutes.tradeRoutes[IO](tradeProg, researchAlg, wsConn).orNotFound
    corsAndErrorsApp = CORS(ErrorHandling(httpApp))
    _ <- Resource.liftF(Database.initialize[IO](transactor))
    // _ <- MyWsClient.reportResponses(wsConn)
    server <- BlazeServerBuilder[IO](global)
              .bindHttp(8080, "0.0.0.0")
              .withHttpApp(corsAndErrorsApp)
              .resource
  } yield server

  def run(args: List[String]) = server.use(_ => IO.never)
}