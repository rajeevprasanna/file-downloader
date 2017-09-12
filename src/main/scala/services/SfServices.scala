package services

import cats._
import cats.implicits._

import java.net.URLEncoder
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.util.ByteString
import models.ErrorModels.SFDCError
import models.SFModels._
import models.SFServiceModels._
import spray.json.JsonReader
import utils.{ActorUtils, DateUtils, HttpUtils}

import scala.concurrent.Future

//TODO:read :https://github.com/calvinlfer/akka-streams-interleaving

/**
  * Created by rajeevprasanna on 7/20/17.
  */
trait SfServices extends HttpUtils with ActorUtils with SFModelsProtocol {

  val sfPayload = HttpEntity.Strict(contentType = ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`),  data = "")
  implicit val oauthHeaders = (accessToken:String) => scala.collection.immutable.Seq(RawHeader("Authorization", s"Bearer ${accessToken}"))

  val validFileExtensions = List("pdf","PDF", "doc", "DOC", "docx", "DOCX","xls", "XLS","xlsx", "XLSX", "ppt","PPT", "pptx", "PPTX")

  class BFUTF8Encoder(val s:String){
    def utf8Encode = URLEncoder.encode(s, "UTF-8")
  }
  implicit def utf8ImplicitEncoder(s:String) = new BFUTF8Encoder(s)

  class SFOauthRequest(val query:String){
    def sfAPI[A](tokens:SalesforceTokens)(implicit j:JsonReader[A]) = {
      val url = s"${tokens.instance_url}/services/data/v39.0/query?q=${query.utf8Encode}"
      Http().singleRequest(HttpRequest(GET, uri = url, entity = sfPayload, headers = tokens.access_token)).flatMap(extractResponse[A])
    }
  }
  implicit def sfQuery(q:String) = new SFOauthRequest(q)

  def getClosedOpportunityIds(implicit tokens:SalesforceTokens):Future[Either[List[SFDCError], SFOpportunityRecords]] = {
    val query = s"Select OpportunityId From OpportunityHistory Where createdDate >=  ${DateUtils.getOldestDate()}  and createdDate < ${DateUtils.getCurrentUTCDateStr()} and probability=100 group by opportunityId"
    query.sfAPI[SFOpportunityRecords](tokens)
  }

  def getOpportunityDetails(oppRecord:SFOpportunityRecords)(tokens:SalesforceTokens) = {
    val ids = oppRecord.records.map(x => s"'${x.OpportunityId}'").mkString(", ")
    val query = s"Select OpportunityId, Opportunity.Name,CreatedDate, StageName, Amount, ExpectedRevenue, CloseDate, Probability  From OpportunityHistory WHERE OpportunityId in ( ${ids})"
    query.sfAPI[SFOpportunityDetailsResp](tokens)
  }

  def getAttachmentDocumentParentIds(oppRecord:SFOpportunityInfo)(tokens:SalesforceTokens):Future[Either[List[SFDCError], SFAttachmentResp]] = {
    val query = s"SELECT ParentId FROM Attachment WHERE ParentId in (${oppRecord.getOpIdsQuery}) GROUP BY ParentId"
    query.sfAPI[SFAttachmentResp](tokens)
  }

  def getAttachmentDocuments(tokens:SalesforceTokens)(oppInfo:SFOpportunityInfo)(attachmentResp:SFAttachmentResp) = {
    val query = s"SELECT Id, Name, Body, CreatedDate, ParentId FROM Attachment WHERE ParentId in (${oppInfo.getOpIdsQuery})"
    query.sfAPI[SFAttachmentInfoResp](tokens)
  }

  def dataFromContentDocumentLink(oppRecord:SFOpportunityInfo)(tokens:SalesforceTokens) = {
    val query = s"SELECT ContentDocumentId, LinkedEntityId FROM ContentDocumentLink WHERE LinkedEntityId  in (${oppRecord.getOpIdsQuery})"
    query.sfAPI[ContentDocumentLinkResp](tokens)
  }

  def getLatestContentVersions(contentResp:ContentDocumentLinkResp)(tokens:SalesforceTokens):Future[Either[List[SFDCError], ContentDocumentVersionResp]] = {
    val ids = contentResp.records.map(_.ContentDocumentId).map(x => s"'${x}'").mkString(", ")
    if(ids.isEmpty){
      val resp = (ContentDocumentVersionResp.empty).asRight[List[SFDCError]]
      Future.successful(resp)
    }else{
      val validExtensions = validFileExtensions.map(x => s"'${x}'").mkString(", ")
      val query = s"SELECT ContentDocumentId, Id, VersionNumber, ContentUrl,Title, Description, CreatedDate, ContentModifiedDate, TagCsv,FileType, VersionData, FileExtension FROM ContentVersion " +
        s"WHERE ContentDocumentId in ($ids) AND isLatest=true AND FileExtension IN ($validExtensions) AND contentLocation = 'S' AND contentSize > 0 "
      query.sfAPI[ContentDocumentVersionResp](tokens)
    }
  }

  def dataFromContentDistribution(oppRecord:SFOpportunityInfo)(tokens:SalesforceTokens) = {
      val query = s"SELECT ContentDocumentId, RelatedRecordId FROM ContentDistribution WHERE RelatedRecordId in (${oppRecord.getOpIdsQuery}) GROUP BY ContentDocumentId, ContentVersionId, RelatedRecordId"
      query.sfAPI[ContentDocumentDistributionResp](tokens)
  }

  private def emptyMessageDigest: MessageDigest = MessageDigest.getInstance("SHA-256")
  private def updateDigest:(MessageDigest, ByteBuffer) => MessageDigest = (digest, bytes) => {
    digest.update(bytes)
    digest
  }
  private def digestToString:MessageDigest => ByteString  = (md) => ByteString(md.digest())
  private def sha256String:(ByteString => String) = hash => DatatypeConverter.printHexBinary(hash.toArray).toLowerCase

  private def calculateDigest(): Sink[ByteBuffer, Future[String]] =
    Flow[ByteBuffer].fold(emptyMessageDigest)(updateDigest)
      .map(digestToString).map(sha256String).toMat(Sink.head[String])(Keep.right)

  private def sha256HashCalculator:HttpResponse => Future[String] =  (_ : HttpResponse).entity.dataBytes.map(_.asByteBuffer).runWith(calculateDigest)
  def getSha256OfURL(url:String, accessToken:String):Future[String] = Http().singleRequest(HttpRequest(GET, uri = url, headers = accessToken)).flatMap(sha256HashCalculator)
}
