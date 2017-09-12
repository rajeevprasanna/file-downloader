package utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext

/**
  * Created by rajeevprasanna on 7/14/17.
  */
trait ActorUtils {

  implicit def system:ActorSystem
  implicit def materializer:ActorMaterializer
  implicit def executionContext:ExecutionContext

}
