package services

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import cats.kernel.Monoid
import models.SFModels._
import models.SFServiceModels._
import cats.syntax.either._
import models.ErrorModels.SFDCError
import utils.ActorUtils

import scala.concurrent.{Future, Promise}

/**
  * Created by rajeevprasanna on 7/28/17.
  */
trait AttachmentService extends SfServices with ActorUtils {

  def getUserOpportunities(tokens:SalesforceTokens):Future[Either[List[SFDCError], SFOpportunityInfo]] = getClosedOpportunityIds(tokens).flatMap(oppIdsEither => oppIdsEither match {
    case Left(errors) => Future.successful(errors.asLeft[SFOpportunityInfo])
    case Right(oppIdRecords) => getOpportunityDetails(oppIdRecords)(tokens).map(x => x.transform)
  })

  def attachmentStreamProcessor(tokens:SalesforceTokens)(attachments:List[Either[List[SFDCError], (SFOpportunityInfo, SFAttachmentInfo)]]):Future[Either[List[SFDCError], List[AttachmentWithStageInfo]]] = {
      val source:Source[Either[List[SFDCError], Option[AttachmentWithStageInfo]], NotUsed] = Source(attachments)
        .mapAsyncUnordered(2){
          case attachmentInfoEither =>
                  val p = Promise[Either[List[SFDCError], Option[AttachmentWithStageInfo]]]
                  attachmentInfoEither match {
                    case Left(errors) => p.success(errors.asLeft[Option[AttachmentWithStageInfo]])
                    case Right(tuple) => getAttachmentStageInfo(tokens)(tuple._1)(tuple._2).map(res => p.success(res.asRight[List[SFDCError]]))
                  }
                  p.future
      }
    source.runFold(initalValue)(combineAttachmentsResp)
  }

  implicit val attachmentStageInfoMonoid = new Monoid[Either[List[SFDCError], List[AttachmentWithStageInfo]]] {
    override def empty: Either[List[SFDCError], List[AttachmentWithStageInfo]] = initalValue
    override def combine(x: Either[List[SFDCError], List[AttachmentWithStageInfo]], y: Either[List[SFDCError], List[AttachmentWithStageInfo]]): Either[List[SFDCError], List[AttachmentWithStageInfo]] = (x, y) match {
      case (Left(erx), Left(ery)) => (erx ++ ery).asLeft[List[AttachmentWithStageInfo]]
      case (Left(erx), _) => erx.asLeft[List[AttachmentWithStageInfo]]
      case (_, Left(ery)) => ery.asLeft[List[AttachmentWithStageInfo]]
      case (Right(resx), Right(resy)) => (resx ++ resy).asRight[List[SFDCError]]
    }
  }

  //TOOD :make this one as monoid
  val initalValue:Either[List[SFDCError], List[AttachmentWithStageInfo]] = List.empty[AttachmentWithStageInfo].asRight[List[SFDCError]]
  def combineAttachmentsResp(x:Either[List[SFDCError], List[AttachmentWithStageInfo]], y:Either[List[SFDCError], Option[AttachmentWithStageInfo]]):Either[List[SFDCError], List[AttachmentWithStageInfo]] = (x, y) match {
    case (Left(erx), Left(ery)) => (erx ++ ery).asLeft[List[AttachmentWithStageInfo]]
    case (Left(erx), _) => erx.asLeft[List[AttachmentWithStageInfo]]
    case (_, Left(ery)) => ery.asLeft[List[AttachmentWithStageInfo]]
    case (Right(resx), Right(None)) => x
    case (Right(resx), Right(Some(resy))) => (resx :+ resy).asRight[List[SFDCError]]
  }

  def getAttachmentStageInfo(tokens:SalesforceTokens)(opportunitiesInfo:SFOpportunityInfo)(attachment:SFAttachmentInfo):Future[Option[AttachmentWithStageInfo]] = {
    val url = s"${tokens.instance_url}${attachment.Body}"
    getSha256OfURL(url, tokens.access_token).map(hash => {
      val update = attachment.copy(ContentHash = Some(hash))
      computeAttachmentStageInfo(update)(opportunitiesInfo)
    })
  }

  def computeAttachmentStageInfo(attachment:SFAttachmentInfo)(opportunitiesInfo:SFOpportunityInfo):Option[AttachmentWithStageInfo] =
      opportunitiesInfo.opMap.get(attachment.ParentId).head.collectFirst({
        case opStageDetails if attachment.CreatedDate.after(opStageDetails.CreatedDate) && opStageDetails.Probability != 100 =>
          AttachmentWithStageInfo(opStageDetails.StageName, opStageDetails.Amount, opStageDetails.CloseDate, opStageDetails.OpportunityId, opStageDetails.Opportunity.Name, opStageDetails.Probability, attachment.Name, attachment.ContentHash)
      })

  def reArrangeAttachmentModels(in:Either[List[SFDCError], (SFOpportunityInfo, SFAttachmentInfoResp)]):List[Either[List[SFDCError], (SFOpportunityInfo, SFAttachmentInfo)]] = {
    in match {
      case Left(errors) => List(errors.asLeft[(SFOpportunityInfo, SFAttachmentInfo)])
      case Right(tuple) => tuple._2.records.map(a => (tuple._1, a).asRight[List[SFDCError]])
    }
  }

  def getAttachmentsInfo(tokens:SalesforceTokens)(oppInfo:Either[List[SFDCError], SFOpportunityInfo]):Future[Either[List[SFDCError], (SFOpportunityInfo, SFAttachmentInfoResp)]] = {
    oppInfo match {
      case Left(errors) => Future.successful(errors.asLeft[(SFOpportunityInfo, SFAttachmentInfoResp)])
      case Right(info) => getAttachmentDocumentParentIds(info)(tokens).flatMap(getAttachmentDetails(info)(tokens))
    }
  }

  private def getAttachmentDetails(oppRecords:SFOpportunityInfo)(tokens:SalesforceTokens)(attachmentIds:Either[List[SFDCError], SFAttachmentResp]):Future[Either[List[SFDCError], (SFOpportunityInfo, SFAttachmentInfoResp)]] =
      attachmentIds match {
        case Left(errors) => Future.successful(errors.asLeft[(SFOpportunityInfo, SFAttachmentInfoResp)])
        case Right(ids) => getAttachmentDocuments(tokens)(oppRecords)(ids).map(attachmentInfo =>
          attachmentInfo match {
            case Left(errors) => errors.asLeft[(SFOpportunityInfo, SFAttachmentInfoResp)]
            case Right(attchments) => (oppRecords, attchments).asRight[List[SFDCError]]
          })
      }



}
