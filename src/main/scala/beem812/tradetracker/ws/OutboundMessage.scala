package beem812.tradetracker.ws

import cats.syntax.functor._
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

object OutboundMessage {
  case class TradeData(s: String, p: Double, t: Double)

  trait OutboundMessage {
    override def toString: String = this.asJson.toString()
  }
  case class Ping(`type`: String, data: String) extends OutboundMessage
  case class TradeMessage(`type`: String, data: List[TradeData]) extends OutboundMessage 
  case class UnknownMessage(`type`: String) extends OutboundMessage
  case class LatestPrice(ticker: String, price: Double) extends OutboundMessage

  implicit val encodeEvent: Encoder[OutboundMessage] = Encoder.instance {
    case ping @ Ping(_,_) => ping.asJson
    case tradeMessage @ TradeMessage(_, _) => tradeMessage.asJson
    case um @ UnknownMessage(_) => um.asJson
    case lp @ LatestPrice(_,_) => lp.asJson
  }

  implicit val decodeOutboundMessage: Decoder[OutboundMessage] =
    List[Decoder[OutboundMessage]](
      Decoder[Ping].widen,
      Decoder[TradeMessage].widen,
    ).reduceLeft(_ or _)

  def parseMessage(txt: String): OutboundMessage = {
    val result = for {
      json <- parse(txt)
      message <- json.as[OutboundMessage]
      finalMessage = message match {
        case TradeMessage(_, data) => {
          val mostRecent = data.sortBy((trade) => trade.t).last
          LatestPrice(mostRecent.s, mostRecent.p)
        }
        case _ => message
      }
    } yield finalMessage
    result.getOrElse(UnknownMessage("unknown message"))
  }

}
