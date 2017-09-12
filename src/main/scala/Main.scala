import java.security.MessageDigest

import com.typesafe.config._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import myDomain.BFUserInfoRetriever
import cats.data._
import cats.implicits._
import models.ErrorModels.SFDCError
import models.SFModels._
import models.SFServiceModels.AttachmentWithStageInfo
import models.UserProfileModels._
import services.{SfServices, SfUtils}
import utils.FnTransformers
import utils.FnTransformers._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object Main extends App with BFUserInfoRetriever with SFDCAnalytics {

  val decider: Supervision.Decider = { e =>
    println(s"Unhandled exception in stream. exception => ${e.getStackTrace.mkString("\n")}")
    Supervision.Stop
  }

  val config = ConfigFactory.load()

  implicit def system:ActorSystem = ActorSystem("SFDC", config)
  val materializerSettings = ActorMaterializerSettings(system).withSupervisionStrategy(decider)
  implicit def materializer:ActorMaterializer = ActorMaterializer(materializerSettings)(system)
  implicit def executionContext = system.dispatchers.lookup("my-dispatcher")

  //Filter users having invalid oauth tokens(tokens which are already marked as invalid in previous iteration)
  def filterInvalidTokenAccounts = (userInfoResp:UserInfoResp) => userInfoResp.result.filter(_.sfOauth.invalidTokens != 1.some)

  val userProfilesOr:Future[Either[List[SFDCError], List[SalesforceUserProfile]]] = EitherT(fetchUsers()).map(filterInvalidTokenAccounts).value
  val userProfilesSource:Source[List[Either[List[SFDCError], SalesforceUserProfile]], NotUsed] = Source.fromFuture(userProfilesOr).map(FnTransformers.rearrangeList)

  val analyticsFlow:Flow[List[Either[List[SFDCError], SalesforceUserProfile]], Either[List[SFDCError], (SalesforceTokens, List[AttachmentWithStageInfo])], _] = Flow[List[Either[List[SFDCError], SalesforceUserProfile]]].mapAsync(3)(extractAnalytics)
  userProfilesSource.via(analyticsFlow).runWith(Sink.foreach(println))




//  val tokens = SalesforceTokens("00D6F000001Ndhj!ASAAQJY1ylV8YGhG64qGwN5QUtULdXVR_03QwFM4wlwJlfFCKspCjzEEXMW.JJerW13yboCiEFo06L04FsLehRVTMDIPOyTJ","k85iVCDHP/nOgQEOQN6m/f0hvyLQrbKWag0sy1cUUZU=","refresh_token id api","https://ap4.salesforce.com","https://login.salesforce.com/id/00D6F000001NdhjUAC/0056F0000065NEvQAM","Bearer","1501064574377")
//  val res = fetchSalesforceAnalytics(tokens).onComplete(x => {
//    x match {
//      case Success(s) => println(s"success => ${s}")
//      case Failure(s) => println(s"error => ${s}")
//    }
//  })

  Thread.sleep(100000000)
//  Await.result(res, 10000000.seconds)
}
