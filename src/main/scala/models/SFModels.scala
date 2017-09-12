package models

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat}
import utils.DateUtils.formatter

/**
  * Created by rajeevprasanna on 7/14/17.
  */
object SFModels {

  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
  val dateFormatterWithoutTime = new SimpleDateFormat("yyyy-MM-dd")
  dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"))
  implicit val strToDate:String => Date = (date) => if(date.length > dateFormatterWithoutTime.toPattern.length) dateFormatter.parse(date) else dateFormatterWithoutTime.parse(date)

  final case class SalesforceTokens(access_token:String, signature:String, scope:String, instance_url:String, id:String, token_type:String, issued_at:String)

  final case class SFOpportunityId(OpportunityId:String)
  final case class SFOpportunityRecords(totalSize:Int, done:Boolean, records:List[SFOpportunityId])

  final case class SFOpportunityName(Name:String)
  final case class SFOpportunityDetails(OpportunityId:String, Opportunity:SFOpportunityName, CreatedDate:String, StageName:String, Amount:Option[Double], ExpectedRevenue:Option[Double], CloseDate:Date, Probability:Double)
  final case class SFOpportunityDetailsResp(totalSize:Int, done:Boolean, records:List[SFOpportunityDetails])

  final case class SFAttachmentParentId(ParentId:String)
  final case class SFAttachmentResp(totalSize:Int, done:Boolean, records:List[SFAttachmentParentId])

  final case class SFAttachmentInfo(Id:String, Name:String, Body:String, CreatedDate:Date, ParentId:String, ContentHash:Option[String])
  final case class SFAttachmentInfoResp(totalSize:Int, done:Boolean, records:List[SFAttachmentInfo])

  final case class ContentDocumentLinkModel(ContentDocumentId:String, LinkedEntityId:String)
  final case class ContentDocumentLinkResp(totalSize:Int, done:Boolean, records:List[ContentDocumentLinkModel])

  final case class ContentDocumentVersion(ContentDocumentId:String, Id:String, VersionNumber:String, ContentUrl:Option[String],
                                          Title:String, Description:Option[String], CreatedDate:Date,
                                          ContentModifiedDate:Date, TagCsv:Option[String], FileType:String, VersionData:String, FileExtension:String, LinkedEntityId:Option[String], ContentHash:Option[String])

  final case class ContentDocumentVersionResp(totalSize:Int, done:Boolean, records:List[ContentDocumentVersion])
  object ContentDocumentVersionResp
  {
    val empty = ContentDocumentVersionResp(0, true, List())
  }

  final case class ContentDocumentDistributionModel(ContentDocumentId:String, RelatedRecordId:String)
  final case class ContentDocumentDistributionResp(totalSize:Int, done:Boolean, records:List[ContentDocumentDistributionModel])

  trait SFModelsProtocol extends SprayJsonSupport with DefaultJsonProtocol {

    implicit object BFDateFormat extends JsonFormat[Date] {
      //TODO : Move this into global variable
      //also keep these formatters into a separate class
      val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
      formatter.setTimeZone(TimeZone.getTimeZone("UTC"))

      override def write(obj: Date): JsValue = JsString(formatter.format(obj))
      override def read(json: JsValue): Date = json match {
        case JsString(rawData) => rawData
        case _ => ""
      }
    }

    implicit val salesforceTokensFormat = jsonFormat7(SalesforceTokens.apply)
    implicit val SFOpportunityIdFormat = jsonFormat1(SFOpportunityId.apply)
    implicit val SFOpportunityRecordsFormat = jsonFormat3(SFOpportunityRecords.apply)
    implicit val SFOpportunityNameFormat = jsonFormat1(SFOpportunityName.apply)
    implicit val SFOpportunityDetailsFormat = jsonFormat8(SFOpportunityDetails.apply)
    implicit val SFOpportunityDetailsRespFormat = jsonFormat3(SFOpportunityDetailsResp.apply)
    implicit val SFAttachmentParentIdFormat = jsonFormat1(SFAttachmentParentId.apply)
    implicit val SFAttachmentRespFormat = jsonFormat3(SFAttachmentResp.apply)
    implicit val SFAttachmentInfoFormat = jsonFormat6(SFAttachmentInfo.apply)
    implicit val SFAttachmentInfoRespFormat = jsonFormat3(SFAttachmentInfoResp.apply)
    implicit val ContentDocumentLinkModelFormat = jsonFormat2(ContentDocumentLinkModel.apply)
    implicit val ContentDocumentLinkRespFormat = jsonFormat3(ContentDocumentLinkResp.apply)
    implicit val ContentDocumentVersionFormat = jsonFormat14(ContentDocumentVersion.apply)
    implicit val ContentDocumentVersionRespFormat = jsonFormat3(ContentDocumentVersionResp.apply)
    implicit val ContentDocumentDistributionModelFormat = jsonFormat2(ContentDocumentDistributionModel.apply)
    implicit val ContentDocumentDistributionRespFormat = jsonFormat3(ContentDocumentDistributionResp.apply)
  }

}
