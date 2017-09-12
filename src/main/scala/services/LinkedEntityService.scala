package services

import akka.NotUsed
import akka.stream.scaladsl.Source
import models.SFModels._
import models.SFServiceModels.{AttachmentWithStageInfo, SFOpportunityInfo}
import utils.ActorUtils
import cats._
import cats.data._
import cats.implicits._
import models.ErrorModels.SFDCError

import scala.concurrent.{Future, Promise}

/**
  * Created by rajeevprasanna on 7/29/17.
  */
trait LinkedEntityService  extends SfServices with ActorUtils with AttachmentService {

  def linkedEntityStreamProcessor(tokens:SalesforceTokens)(entities:List[Either[List[SFDCError], (SFOpportunityInfo, ContentDocumentVersion)]]):Future[Either[List[SFDCError], List[AttachmentWithStageInfo]]] = {
    val source:Source[Either[List[SFDCError], Option[AttachmentWithStageInfo]], NotUsed] = Source(entities)
      .mapAsyncUnordered(2){
        case entityInfoEither =>
          val p = Promise[Either[List[SFDCError], Option[AttachmentWithStageInfo]]]
          entityInfoEither match {
            case Left(errors) => p.success(errors.asLeft[Option[AttachmentWithStageInfo]])
            case Right(tuple) => getEntityStageInfo(tokens)(tuple._1)(tuple._2).map(res => p.success(res.asRight[List[SFDCError]]))
          }
          p.future
      }
    source.runFold(initalValue)(combineAttachmentsResp)
  }

  def getEntityStageInfo(tokens:SalesforceTokens)(opportunitiesInfo:SFOpportunityInfo)(entity:ContentDocumentVersion):Future[Option[AttachmentWithStageInfo]] = {
    val url = s"${tokens.instance_url}${entity.VersionData}"
//    println(s"downloading entity file with url ===> ${url}")
    getSha256OfURL(url, tokens.access_token).map(hash => {
//      println(s"${hash} of file with url => ${url}")
      val update = entity.copy(ContentHash = Some(hash))
      computeEntityStageInfo(update)(opportunitiesInfo).map(x => {
        //TODO :check code is not working when println is commented
        println(s"getEntityStageInfo => ${x}")
        x
      })
    })
  }

  def computeEntityStageInfo(entity:ContentDocumentVersion)(opportunitiesInfo:SFOpportunityInfo):Option[AttachmentWithStageInfo] =
    opportunitiesInfo.opMap.get(entity.LinkedEntityId.get).head.collectFirst({
      case opStageDetails if entity.CreatedDate.after(opStageDetails.CreatedDate) && opStageDetails.Probability != 100 =>
        AttachmentWithStageInfo(opStageDetails.StageName, opStageDetails.Amount, opStageDetails.CloseDate, opStageDetails.OpportunityId, opStageDetails.Opportunity.Name, opStageDetails.Probability, entity.Title, entity.ContentHash)
    })


  //RE use this method in other service
  def reArrangeEntityModels(in:Either[List[SFDCError], (SFOpportunityInfo, ContentDocumentVersionResp)]):List[Either[List[SFDCError], (SFOpportunityInfo, ContentDocumentVersion)]] = {
    in match {
      case Left(errors) => List(errors.asLeft[(SFOpportunityInfo, ContentDocumentVersion)])
      case Right(tuple) => tuple._2.records.map(a => (tuple._1, a).asRight[List[SFDCError]])
    }
  }

  def getLinkedEntities(tokens:SalesforceTokens)(oppInfo:Either[List[SFDCError], SFOpportunityInfo]):Future[Either[List[SFDCError], (SFOpportunityInfo, ContentDocumentVersionResp)]] = {
    oppInfo match {
      case Left(errors) => Future.successful(errors.asLeft[(SFOpportunityInfo, ContentDocumentVersionResp)])
      case Right(info) => dataFromContentDocumentLink(info)(tokens).flatMap(getLatestContentInfo(tokens)(info))
    }
  }

  def convertDistToLinkResp = (resp:Either[List[SFDCError], ContentDocumentDistributionResp]) => resp.map(res => ContentDocumentLinkResp(res.totalSize, res.done, res.records.map(r => ContentDocumentLinkModel(r.ContentDocumentId, r.RelatedRecordId))))

  def getDistributedEntities(tokens:SalesforceTokens)(oppInfo:Either[List[SFDCError], SFOpportunityInfo]):Future[Either[List[SFDCError], (SFOpportunityInfo, ContentDocumentVersionResp)]] = {
    oppInfo match {
      case Left(errors) => Future.successful(errors.asLeft[(SFOpportunityInfo, ContentDocumentVersionResp)])
      case Right(info) => dataFromContentDistribution(info)(tokens).map(convertDistToLinkResp).flatMap(getLatestContentInfo(tokens)(info))
    }
  }

 private def getLatestContentInfo(tokens:SalesforceTokens)(oppRecords:SFOpportunityInfo)(linkedEntityIdsResp:Either[List[SFDCError], ContentDocumentLinkResp]):Future[Either[List[SFDCError], (SFOpportunityInfo, ContentDocumentVersionResp)]] =
   linkedEntityIdsResp match {
     case Left(errors) =>  Future.successful(errors.asLeft[(SFOpportunityInfo, ContentDocumentVersionResp)])
     case Right(contentResp) => getLatestContentVersions(contentResp)(tokens).flatMap(contentVersionRespEr =>
      contentVersionRespEr match  {
         case Left(errors) => Future.successful(errors.asLeft[(SFOpportunityInfo, ContentDocumentVersionResp)])
         case Right(contentVersions) =>
           def fetchLinkedId = (version:ContentDocumentVersion) => contentResp.records.collectFirst{case model if model.ContentDocumentId == version.ContentDocumentId => model.LinkedEntityId}
           val updatedVersions = contentVersions.records.map(version => version.copy(LinkedEntityId = fetchLinkedId(version)))
           Future.successful((oppRecords, contentVersions.copy(records=updatedVersions)).asRight[List[SFDCError]])
       }
     )
  }

}
