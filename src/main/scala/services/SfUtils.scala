package services

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model._
import cats.implicits._
import constants.endpoints._
import models.ErrorModels.SFDCError
import models.SFModels.{SFModelsProtocol, SalesforceTokens}
import models.UserProfileModels.SalesforceUserProfile
import utils.{ActorUtils, HttpUtils}

import scala.concurrent.Future


/**
  * Created by rajeevprasanna on 7/12/17.
  */
trait SfUtils extends HttpUtils with ActorUtils with SFModelsProtocol {

  def refreshSFTokens(userOr:Either[List[SFDCError], SalesforceUserProfile]):Future[Either[List[SFDCError], SalesforceTokens]] = {
    val res:Either[List[SFDCError], Future[Either[List[SFDCError], SalesforceTokens]]] = userOr.map(user => {
      assert(user.sfOauth.refresh_token != none)
      val payload = HttpEntity.Strict(contentType = ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`),  data = sfPayload(user))
      Http().singleRequest(HttpRequest(POST, uri = SF_Refresh_Tokens, entity = payload)).flatMap(extractResponse[SalesforceTokens])
    })

    res match {
      case Left(errors) => Future.successful(errors.asLeft[SalesforceTokens])
      case Right(resp) => resp
    }
  }

  private def sfPayload(user:SalesforceUserProfile) = s"""grant_type=refresh_token&refresh_token=${user.sfOauth.refresh_token.get}&client_id=3MVG9szVa2RxsqBZXqb3eVmQkhktA0I16Ydw4I2AW3jqo9h3WxjC8vumvRIFO0nIi86pUhYyB06PhpN61gGF4&client_secret=2963668367839070738"""
}
