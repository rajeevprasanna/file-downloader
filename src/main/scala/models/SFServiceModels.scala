package models

import java.util.Date

import models.ErrorModels.SFDCError
import models.SFModels._


/**
  * Created by rajeevprasanna on 7/28/17.
  */
object SFServiceModels {

  final case class SFOpportunityInfo(opMap:Map[String, List[SFOpportunityDetails]]) {
    def getOpIds = opMap.keys
    def getOpIdsQuery = getOpIds.map(id => s"""'${id}'""").mkString(", ")
  }

  final case class AttachmentWithStageInfo(StageName:String, Amount:Option[Double], CloseDate:Date, OpportunityId:String, OpporunityName:String, Probability:Double, FileName:String, ContentHash:Option[String])

  implicit class OpModelConverter(in:Either[List[SFDCError], SFOpportunityDetailsResp]){
    def transform: Either[List[SFDCError], SFOpportunityInfo] = in.map(_.records.groupBy(_.OpportunityId).map(x => (x._1, x._2.sortBy(_.CreatedDate).reverse))).map(details => SFOpportunityInfo(details))
  }
}
