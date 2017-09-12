//http://blog.leifbattermann.de/2017/03/16/7-most-convenient-ways-to-create-a-future-either-stack/

import cats._
import cats.data._
import cats.implicits._

import scala.concurrent
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

val resultx:Future[Either[List[String], Int]] = Future.successful(2.asRight)
val r:Future[Int] = for {
                      x:Either[List[String], Int] <- resultx
                    } yield x.getOrElse(-1)

Await.result(r, 10.seconds)

//resultx.flatMap{eitherList =>
//  println("Read variable")
//  eitherList.map(y => println(s"y ===> $y"))
//  Future.successful("Test")
//}


//val temp:Either[String, Int] = 3.asRight[String]



//val resulty:Future[Either[List[String], Int]] = Future.successful(3.asRight)
//val res:EitherT[Future, List[String], Int]  = for {
//            x <- EitherT(resultx)
//            y <- EitherT(resulty)
//          } yield {
//            println(s"x ===> ${x}")
//            x+y
//          }
//
//val resx:Future[Either[List[String], Int]] = res.value



//
//type Error = String
//type ErrorOr[A] = Either[Error, A]
//type FutureEither[A] = EitherT[Future, Error, A]
//
//Right(42):Either[Error, Int]
//42.pure[FutureEither]
//
//val v = 42.pure[ErrorOr]
//EitherT.fromEither[Future](v)
//
//val vx = "foo".pure[Future]
//EitherT.right(vx)
//
//
////Use the EitherT constructor to create a FutureEither[A] from a Future[Either[Error, A]]:
//
//import cats.data.OptionT
//type ListOption[A] = OptionT[List, A]
//
//import cats.Monad
//import cats.instances.list._
//import cats.syntax.applicative._
//
//val result1:ListOption[Int] = 42.pure[ListOption]
//












