package beem812.tradetracker.ws

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

object UserMessage {
  
  case class SubscriptionMessage(symbol: String, `type`: String){
    override def toString: String = this.asJson.toString()
  }

  sealed trait UserMessage {
    def toString: String
  }
  case class BadMessage() extends UserMessage {
    override def toString: String = ""
  }
  case class TickerSubscriptionMessage(message: SubscriptionMessage) extends UserMessage {
    override def toString: String = message.asJson.toString()
  }

  def parseUserMessage(txt: String): UserMessage = {
    val thing = for {
      json <- parse(txt)
      message <- json.as[SubscriptionMessage]
    } yield TickerSubscriptionMessage(message)

    thing.getOrElse(BadMessage())
  }

  def badMessage() = BadMessage()
}