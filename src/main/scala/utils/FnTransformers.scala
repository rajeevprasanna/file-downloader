package utils

import cats._
import cats.data._
import cats.syntax._
import cats.implicits._
import cats.functor._
import models.ErrorModels.SFDCError

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by rajeevprasanna on 7/22/17.
  */
object FnTransformers {

  def futureFlatMap[A, B]:(Either[A, Future[Either[A, B]]] => Future[Either[A, B]]) = (p:Either[A, Future[Either[A, B]]]) => {
    val promise = Promise[Either[A, B]]()
    p match {
      case Left(x) => promise.success(x.asLeft[B])
      case Right(y) => y.map(z => {
        promise.success(z)
        Future("")
      })
    }
    promise.future
  }

  def rearrangeList[A]:Either[List[SFDCError], List[A]] => List[Either[List[SFDCError], A]] = (in:Either[List[SFDCError], List[A]]) =>
    in match {
      case Left(errors) => List(errors.asLeft[A])
      case Right(list) => list.map(el => el.asRight[List[SFDCError]])
    }

  def extractModelsFromEithers[A, B]: (Either[List[SFDCError], A], Either[List[SFDCError], B]) => Either[List[SFDCError], (A, B)] = (a:Either[List[SFDCError], A], b:Either[List[SFDCError], B]) => (a, b) match {
    case (Left(errors1), Left(errors2)) => (errors1 |+| errors2).asLeft[(A, B)]
    case (Left(errors1), _) => errors1.asLeft[(A, B)]
    case (_, Left(errors2)) => errors2.asLeft[(A, B)]
    case (Right(x), Right(y)) => (x, y).asRight[List[SFDCError]]
  }

}
