package temp

/**
  * Created by rajeevprasanna on 7/10/17.
  */
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.util.{Failure, Success}

final case class Meta(units: String, theme: String, name: String)
final case class AverageSpeed(meta: Meta, data: Map[String, Double])
final case class SensorData(averageSpeed:AverageSpeed)
final case class Sensor(name:String, data:SensorData)

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

//Refer :http://jonnylaw.github.io/blog/post/AkkaClient/

trait Protocols extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val metaFormat:RootJsonFormat[Meta] = jsonFormat(Meta.apply, "units", "theme", "name")
  implicit val averageSpeedFormat:RootJsonFormat[AverageSpeed]  =  jsonFormat(AverageSpeed.apply, "meta", "data")
  implicit val sensorDataFormat:RootJsonFormat[SensorData] = jsonFormat(SensorData.apply, "Average Speed")
  implicit val sensorFormat: RootJsonFormat[Sensor] = jsonFormat(Sensor.apply, "name", "data")
}

object jsonParsingEx extends App with Protocols {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val uri = Uri("http://uoweb1.ncl.ac.uk/api/v1/sensor/data/raw.json")
  val api_key = "5yukn1stvbg8yc74ozs4930ugfmk3ze8iilxr0am3bvr8i8lhp6h6iqkyxr8wxzjq0m5ra8bz03iq5xstfmqwkoqjv"

  val query = Query("api_key" -> api_key,
    "sensor_name" -> "N05171T",
    "start_time" -> "20170201",
    "end_time" -> "20170202",
    "variable" -> "average speed")

  val res: Future[HttpResponse] =  Http().singleRequest(HttpRequest(GET, uri = uri.withQuery(query)))
  res andThen {
    case Success(response) =>
      val resp = response.entity.dataBytes.map(_.utf8String)
      resp.map(_.parseJson.convertTo[List[Sensor]]).runForeach(println)

    case Failure(ex) => println(ex)
  } onComplete {
    _ => system.terminate()
  }

}
