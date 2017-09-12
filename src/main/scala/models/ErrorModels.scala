package models

/**
  * Created by rajeevprasanna on 8/22/17.
  */
object ErrorModels {
  sealed trait SFDCError extends Product with Serializable
  final case class InvalidSFTokens(errorMsg:String) extends SFDCError
  final case class InvalidRequest(errorMsg:String) extends SFDCError
  final case class APIError(errorMsg:String) extends SFDCError
  final case object UnexpectedError extends SFDCError
}
