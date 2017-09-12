package constants

import akka.http.scaladsl.model.Uri

/**
  * Created by rajeevprasanna on 7/14/17.
  */
object endpoints {

  implicit def toURI = (url:String) => Uri(url)

  val SF_User_Details = "https://domain.test.com/salesforce/admin/get"
  val SF_Refresh_Tokens = "https://login.salesforce.com/services/oauth2/token"

}
