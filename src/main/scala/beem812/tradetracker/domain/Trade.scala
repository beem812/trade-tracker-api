package beem812.tradetracker.domain

import java.util.Date
import io.getquill.MappedEncoding
import cats.effect.Sync
import org.http4s.circe.{ jsonEncoderOf, jsonOf }
import io.circe._
import org.http4s._
import java.time.LocalDateTime
object Action extends Enumeration {
  type Action = Value
  val CSP = Value("CSP") 
  val CC = Value("CC")
  val SharesCalled = Value("SharesCalled")
  val SharesAssigned = Value("SharesAssigned")
  implicit val encodeAction = MappedEncoding[Action, String](_.toString())
  implicit val decodeAction = MappedEncoding[String, Action]{
    case "CSP" => Action.CSP
    case "CC" => Action.CC
    case "SharesCalled" => Action.SharesCalled
    case "SharesAssigned" => Action.SharesAssigned
  }

  implicit val actionEncoder: Encoder[Action] = Encoder.instance((a) => Encoder.encodeString(a.toString()))
  implicit val actionDecoder: Decoder[Action] = Decoder.instance(a => Decoder.decodeString(a).map((a) => a match {
    case "CSP" => Action.CSP
    case "CC" => Action.CC
    case "SharesCalled" => Action.SharesCalled
    case "SharesAssigned" => Action.SharesAssigned
  }))
}

object CreditDebit extends Enumeration {
  type CreditDebit = Value
  val Credit = Value("Credit")
  val Debit = Value("Debit")
  implicit val encodeCreditDebit = MappedEncoding[CreditDebit, String](_.toString())
  implicit val decodeCreditDebit = MappedEncoding[String, CreditDebit]{
    case "Credit" => CreditDebit.Credit
    case "Debit" => CreditDebit.Debit
  }
  implicit val creditDebitEncoder: Encoder[CreditDebit] = Encoder.instance((a) => Encoder.encodeString(a.toString()))
  implicit val CreditDebitDecoder: Decoder[CreditDebit] = Decoder.instance(a => Decoder.decodeString(a).map((a) => a match {
    case "Credit" => CreditDebit.Credit
    case "Debit" => CreditDebit.Debit
  }))
}

object trade {
  import Action._
  import CreditDebit._

  implicit def jsonDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]
  implicit def jsonEncoder[F[_]: Sync, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
  implicit val dateDecoder: Decoder[LocalDateTime] = Decoder.instance(a => Decoder.decodeString(a).map((a) => LocalDateTime.parse(a)))
  implicit val dateEncoder: Encoder[LocalDateTime] = Encoder.instance((a: LocalDateTime) => Encoder.encodeString(a.toString()))

  case class Trade(ticker: String, cost: BigDecimal, amount: BigDecimal, date: Date, status: String)

  case class WheelTrade(id: Option[String], ticker: String, action: Action, date: String, pricePerShare: BigDecimal, shares: Int, creditDebit: CreditDebit)

  case class CostBasisData(credit: Double, shares: Double, costBasis: Double)

}