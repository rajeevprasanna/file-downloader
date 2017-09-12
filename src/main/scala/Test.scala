import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


object Test extends App {

  implicit def system:ActorSystem = ActorSystem()
  implicit def materializer:ActorMaterializer = ActorMaterializer()
  implicit def executionContext = system.dispatcher

  val url = s"http://localhost:8000/attach.jpg"

  def emptyMessageDigest: MessageDigest = MessageDigest.getInstance("SHA-256")
  def updateDigest:(MessageDigest, ByteBuffer) => MessageDigest = (digest, bytes) => {
    digest.update(bytes)
    digest
  }
  def digestToString:MessageDigest => ByteString  = (md) => ByteString(md.digest())
  def sha256String:(ByteString => String) = hash => DatatypeConverter.printHexBinary(hash.toArray).toLowerCase

  def calculateDigest(): Sink[ByteBuffer, Future[String]] =
    Flow[ByteBuffer].fold(emptyMessageDigest)(updateDigest)
      .map(digestToString).map(sha256String).toMat(Sink.head[String])(Keep.right)

  def sha256HashCalculator:HttpResponse => Future[String] =  (_ : HttpResponse).entity.dataBytes.map(_.asByteBuffer).runWith(calculateDigest)
  val res = Http().singleRequest(HttpRequest(GET, uri = url)).flatMap(sha256HashCalculator)
  Await.result(res, 1000.seconds)

  res.map(println)

}
