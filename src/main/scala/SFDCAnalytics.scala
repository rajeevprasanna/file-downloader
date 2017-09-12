import akka.NotUsed
import cats._
import cats.data._
import cats.implicits._
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}
import myDomain.BFUserInfoRetriever
import cats.data.EitherT
import models.ErrorModels.SFDCError
import models.SFModels._
import models.SFServiceModels.{AttachmentWithStageInfo, SFOpportunityInfo}
import models.UserProfileModels.SalesforceUserProfile
import services._
import utils.FnTransformers._

import scala.concurrent.Future

/**
  * Created by rajeevprasanna on 7/27/17.
  */
trait SFDCAnalytics extends SfUtils with SfServices with AttachmentService with LinkedEntityService {

  def extractAnalytics:List[Either[List[SFDCError], SalesforceUserProfile]] => Future[Either[List[SFDCError], (SalesforceTokens, List[AttachmentWithStageInfo])]] = (userProfiles:List[Either[List[SFDCError], SalesforceUserProfile]]) => {
    val res:Future[Either[List[SFDCError], (SalesforceTokens, List[AttachmentWithStageInfo])]] = Source(userProfiles).mapAsync(3)(refreshSFTokens).mapAsync(1)(querySFAnalytics).runWith(Sink.head)
    res
  }

  private def querySFAnalytics:Either[List[SFDCError], SalesforceTokens] => Future[Either[List[SFDCError], (SalesforceTokens, List[AttachmentWithStageInfo])]] = (tokensOr:Either[List[SFDCError], SalesforceTokens]) => {
    tokensOr map fetchSalesforceAnalytics match {
      case Left(errors) => Future.successful(errors.asLeft[(SalesforceTokens, List[AttachmentWithStageInfo])])
      case Right(res) => res.map(stageInfoOr => extractModelsFromEithers(tokensOr, stageInfoOr))
    }
  }

  private def fetchSalesforceAnalytics: SalesforceTokens => Future[Either[List[SFDCError], List[AttachmentWithStageInfo]]] = (tokens:SalesforceTokens) => {

    val opportunityInfoSource:Source[Either[List[SFDCError], SFOpportunityInfo], NotUsed] = Source.fromFuture(getUserOpportunities(tokens))

    val attachmentRetrievalFlow:Flow[Either[List[SFDCError], SFOpportunityInfo], Either[List[SFDCError], (SFOpportunityInfo, SFAttachmentInfoResp)], NotUsed] =
      Flow[Either[List[SFDCError], SFOpportunityInfo]].mapAsync(3)(getAttachmentsInfo(tokens))

    val attachmentStageInfoFetcher:Flow[Either[List[SFDCError], (SFOpportunityInfo, SFAttachmentInfoResp)], Either[List[SFDCError], List[AttachmentWithStageInfo]], _] = Flow[Either[List[SFDCError], (SFOpportunityInfo, SFAttachmentInfoResp)]]
      .map(reArrangeAttachmentModels).mapAsync(2)(attachmentStreamProcessor(tokens))

    val linkedEntityIds:Flow[Either[List[SFDCError], SFOpportunityInfo], Either[List[SFDCError], (SFOpportunityInfo, ContentDocumentVersionResp)], _] =
      Flow[Either[List[SFDCError], SFOpportunityInfo]].mapAsync(3)(getLinkedEntities(tokens))

    val distributedEntityIds:Flow[Either[List[SFDCError], SFOpportunityInfo], Either[List[SFDCError], (SFOpportunityInfo, ContentDocumentVersionResp)], _] =
      Flow[Either[List[SFDCError], SFOpportunityInfo]].mapAsync(3)(getDistributedEntities(tokens))

    val entityStageInfoFetcher:Flow[Either[List[SFDCError], (SFOpportunityInfo, ContentDocumentVersionResp)], Either[List[SFDCError], List[AttachmentWithStageInfo]], _] = Flow[Either[List[SFDCError], (SFOpportunityInfo, ContentDocumentVersionResp)]]
      .map(reArrangeEntityModels).mapAsync(3)(linkedEntityStreamProcessor(tokens))

    val out = Sink.fold[Either[List[SFDCError], List[AttachmentWithStageInfo]], Either[List[SFDCError], List[AttachmentWithStageInfo]]](attachmentStageInfoMonoid.empty)(attachmentStageInfoMonoid.combine)

    val graph = RunnableGraph.fromGraph(GraphDSL.create(out){  implicit builder  =>
      sink =>
        import GraphDSL.Implicits._

        val bcast = builder.add(Broadcast[Either[List[SFDCError], SFOpportunityInfo]](3))
        val merger = builder.add(Merge[Either[List[SFDCError], List[AttachmentWithStageInfo]]](3))

        opportunityInfoSource ~>  bcast  ~> attachmentRetrievalFlow     ~> attachmentStageInfoFetcher   ~> merger  ~> sink
                                  bcast ~> linkedEntityIds              ~> entityStageInfoFetcher       ~> merger
                                  bcast ~> distributedEntityIds         ~> entityStageInfoFetcher       ~> merger

        ClosedShape
    })

    graph.run()
  }
}
