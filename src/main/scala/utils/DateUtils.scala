package utils

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

/**
  * Created by rajeevprasanna on 7/20/17.
  */
object DateUtils {

  val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'+00:00'")
  formatter.setTimeZone(TimeZone.getTimeZone("UTC"))

  def getOldestDate = () => formatter.format(new Date(0))
  def getCurrentUTCDateStr = () => formatter.format(new Date())
}
