package myDomain

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model._
import cats.implicits._
import constants.endpoints._
import models.UserProfileModels.{UserInfoResp, UserProfileProtocols}
import utils.{ActorUtils, HttpUtils}

import scala.concurrent.Future

/**
  * Created by rajeevprasanna on 7/10/17.
  */
trait BFUserInfoRetriever extends UserProfileProtocols with HttpUtils with ActorUtils {

  val payload = HttpEntity.Strict(
    contentType = ContentTypes.`application/json`,
    data = s"""{"secret":"f6bdcbfe36bc2635423842387498235683745837543785"}"""
  )

  def fetchUsers() = Http().singleRequest(HttpRequest(POST, uri = SF_User_Details, entity = payload)).flatMap(extractResponse[UserInfoResp])
}





