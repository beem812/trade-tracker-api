package beem812.tradetracker.config

import cats.effect.{Blocker, ContextShift, Resource}
import com.typesafe.config.ConfigFactory
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._
import cats.effect.Sync

object Config {
  case class DatabaseConfig(driver: String, url: String, user: String, password: String, threadPoolSize: Int)
  case class Config(dbConfig: DatabaseConfig)
  def load[F[_]: Sync](configFile: String = "application.conf")(implicit cs: ContextShift[F]): Resource[F, Config] = 

    Blocker[F].flatMap{ blocker => Resource.liftF(ConfigSource.fromConfig(ConfigFactory.load(configFile)).loadF[F, Config](blocker))}
}