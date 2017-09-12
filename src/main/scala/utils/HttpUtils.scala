package utils

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, ResponseEntity, StatusCodes}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import models.UserProfileModels.UserInfoResp

import scala.concurrent.{ExecutionContext, Future}
import spray.json._
import cats.implicits._
import models.ErrorModels.{APIError, SFDCError}

/**
  * Created by rajeevprasanna on 7/14/17.
  */
trait HttpUtils {

  implicit def system:ActorSystem
  implicit def materializer:ActorMaterializer
  implicit def executionContext:ExecutionContext
  implicit  def toByteString = (s:String) => ByteString(s)

  type ErrorsOr[A] = Either[List[SFDCError], A]
  type FutureErrorsOr[A] = Future[ErrorsOr[A]]
  def extractResponse[A](resp:HttpResponse)(implicit j:JsonReader[A]) = resp match {
    case HttpResponse(StatusCodes.OK, _, entity, _) => respToString(entity).map(userInfoParser[A]).flatMap(userInfoResp => Future.successful(userInfoResp.pure[ErrorsOr]))
    case HttpResponse(_, _, entity, _) =>
      respToString(entity).map(error => {
        println(s"error =-==> ${error}")
//        List(error).asLeft
        List(APIError(error)).asLeft
      })
  }

  private def respToString:(ResponseEntity => Future[String]) = (entity:ResponseEntity) => entity.dataBytes.runFold(ByteString.empty) { case (acc, b) => acc ++ b }.map(_.utf8String)
  private def userInfoParser[T](response:String)(implicit j:JsonReader[T]) = {
//    println(s"response ===> ${response}")
    response.parseJson.convertTo[T]
  }

}
