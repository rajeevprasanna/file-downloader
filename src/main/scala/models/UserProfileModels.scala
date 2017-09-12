package models

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import models.SFModels.SalesforceTokens
import models.SFServiceModels.AttachmentWithStageInfo
import spray.json.DefaultJsonProtocol

/**
  * Created by rajeevprasanna on 7/10/17.
  */
object UserProfileModels {

  final case class BFUserInfo(sfUserProfile:SalesforceUserProfile, tokens:SalesforceTokens, attachmentInfo:List[AttachmentWithStageInfo])

  final case class SFOauthModel(access_token:Option[String],
                                admin:Option[Boolean],
                                bootstrap_completed_flag:Option[Boolean],
                                bootstrap_earliest_date : Option[String],
                                bootstrap_run_date:Option[String],
                                bootstrap_start_time:Option[String],
                                content_delivery_permission:Option[Int],
                                disable:Option[Int],
                                email_id:String,
                                instance_url:String,
                                last_run_date:Option[String],
                                refresh_token:Option[String],
                                salesforce_id:String,
                                invalidTokens:Option[Int]
                               )

  final case class SalesforceUserProfile(bfUserEmailId:String, sfOauth:SFOauthModel)
  final case class UserInfoResp(result:List[SalesforceUserProfile], status:String)

  trait UserProfileProtocols extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val SFOauthModelFormat = jsonFormat(SFOauthModel.apply,
                                                    "access_token",
                                                    "admin",
                                                    "bootstrap_completed_flag",
                                                    "bootstrap_earliest_date",
                                                    "bootstrap_run_date",
                                                    "bootstrap_start_time",
                                                    "content_delivery_permission",
                                                    "disable",
                                                    "email_id",
                                                    "instance_url",
                                                    "last_run_date",
                                                    "refresh_token",
                                                    "salesforce_id",
                                                    "invalid_tokens"
                                                  )
    implicit val SalesforceUserProfileFormat = jsonFormat(SalesforceUserProfile.apply, "email_id", "salesforce_oauth")
    implicit val UserInfoRespFormat = jsonFormat2(UserInfoResp.apply)
  }

}