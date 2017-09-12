import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import models.UserProfileModels.{SalesforceUserProfile, UserInfoResp}

import scala.concurrent.{ExecutionContext, Future}

import cats._
import cats.data._
import cats.syntax._
import cats.implicits._

import spray.json._


val map = Map("a" -> 1)

Marshal("a")
Marshal(map).to[HttpEntity]

