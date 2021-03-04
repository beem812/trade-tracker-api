package beem812.tradetracker

import cats.effect.{ExitCode, IO, IOApp}
import beem812.tradetracker.config.Config
import scala.concurrent.ExecutionContext.global
import beem812.tradetracker.algebras.LiveTradesRepo
import beem812.tradetracker.programs.LiveTradeTracker
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware._
import beem812.tradetracker.algebras.LiveAnalysis
// import scala.concurrent.duration._

object Main extends IOApp {
  def run(args: List[String]) = Config.load[IO]().use{ cfg => 
    // val corsConf = CORSConfig(
    //   anyOrigin = false, 
    //   allowedOrigins = Set("http://localhost:8280"), 
    //   allowedMethods = Some(Set("GET", "POST")),
    //   allowCredentials = false, 
    //   maxAge = 1.day.toSeconds, 
    //   allowedHeaders = Some(Set("Access-Control-Allow-Headers", "Content-Type", "Access-Control-Allow-Origin")))
    
    Database.transactor[IO](cfg.dbConfig, global).use{transactor => 
      for {
        tradeAlg <- LiveTradesRepo.make[IO]()
        analysisAlg <- LiveAnalysis.make[IO]()
        tradeProg <- LiveTradeTracker.make[IO](tradeAlg, analysisAlg, transactor)
        httpApp = TradetrackerRoutes.tradeRoutes[IO](tradeProg).orNotFound
        corsAndErrorsApp = CORS(ErrorHandling(httpApp))
        _ <- Database.initialize[IO](transactor)
        _ <- BlazeServerBuilder[IO](global).bindHttp(8080, "0.0.0.0").withHttpApp(corsAndErrorsApp).serve.compile.drain
      } yield ExitCode.Success
    }
  }
}