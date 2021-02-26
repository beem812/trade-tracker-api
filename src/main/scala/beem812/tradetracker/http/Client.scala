package beem812.tradetracker.http

import org.http4s.client.blaze.BlazeClientBuilder
import cats.effect.ConcurrentEffect
import scala.concurrent.ExecutionContext.global

object Client {
  def make[F[_]: ConcurrentEffect]() = {
    BlazeClientBuilder[F](global).resource
  }

}