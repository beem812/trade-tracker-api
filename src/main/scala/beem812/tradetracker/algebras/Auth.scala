package beem812.tradetracker.algebras
import pdi.jwt.{JwtCirce, JwtAlgorithm, JwtClaim, JwtJsonCommon}
import com.auth0.jwk.UrlJwkProvider
import beem812.tradetracker.config.Config
import cats.effect.Sync
import cats.implicits._

/**
 * The Auth trait provides methods for handling authentication and authorization
 * using JSON Web Tokens (JWT).
 */
trait Auth[F[_]] {
  def getJwk(token: String)
}

/**
 * The LiveAuth class implements the Auth trait and provides concrete
 * implementations for handling authentication and authorization.
 */
final class LiveAuth[F[_]: Sync](cfg: Config.Config) extends Auth[F] {

  /**
   * Retrieves the JSON Web Key (JWK) for a given token and decodes the token
   * to extract the claims.
   *
   * @param token The JWT token.
   * @return The decoded claims.
   */
  def getJwk(token: String) {
    val thing = Sync[F].fromTry(JwtCirce.decodeAll(token))
    val jwkProvider = new UrlJwkProvider(cfg.auth0Config.domain)

    val jwk = for {
      (header, _, _) <- thing
      jwk <- Sync[F].delay { jwkProvider.get(header.keyId.getOrElse("")) }
      claims <- Sync[F].fromTry(JwtCirce.decode(token, jwk.getPublicKey(), Seq(JwtAlgorithm.RS256)))
    } yield claims
  }
}
