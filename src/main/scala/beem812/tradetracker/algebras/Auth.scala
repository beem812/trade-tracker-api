package beem812.tradetracker.algebras
import pdi.jwt.{JwtCirce, JwtAlgorithm, JwtClaim, JwtJsonCommon}
import com.auth0.jwk.UrlJwkProvider
import beem812.tradetracker.config.Config
import cats.effect.Sync
import cats.implicits._

trait Auth[F[_]] {
  def getJwk(token: String) 
}

final class LiveAuth[F[_]: Sync](cfg: Config.Config) extends Auth[F] {
  def getJwk(token: String) {
    val thing = Sync[F].fromTry( JwtCirce.decodeAll(token))
    val jwkProvider = new UrlJwkProvider(cfg.auth0Config.domain)

    val jwk = for {
      (header, _, _) <- thing
      jwk <- Sync[F].delay{jwkProvider.get(header.keyId.getOrElse(""))}
      claims <- Sync[F].fromTry(JwtCirce.decode(token, jwk.getPublicKey(), Seq(JwtAlgorithm.RS256)))
    } yield claims 
  }
}