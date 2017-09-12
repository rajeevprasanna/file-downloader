import java.text.SimpleDateFormat
import java.util.{Date, SimpleTimeZone, TimeZone}

import cats._
import cats.data._
import cats.syntax._
import cats.implicits._
import cats.functor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import cats.syntax.applicative._


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

def testFn():Future[Either[String, Int]] = Future.successful(2.asRight[String])
def test2Fn:Int => Future[Either[String, Int]] = (x:Int) => Future.successful((x*2).asRight[String])
val testRes:Future[Either[String, Int]] = EitherT(testFn).map(test2Fn).value.flatMap(futureFlatMap)


//Here i added test functions to return Either[List[String], A] where left is collecting error list.
//
//case class User(name:String)
//case class Users(ppl:List[User])
//
//val testUsers = Users(List(User("test1"), User("test2")))
//
//val func0:Future[Either[List[String], Users]] = Future.successful(testUsers.asRight[List[String]])
//val func1:(Users => Either[List[String], User]) = (users:Users) => users.ppl(0).asRight[List[String]]
//
////How to make this function to return Future[Either[List[String], User]] = ???
//val res:Future[Either[List[String], Either[List[String], User]]] = EitherT(func0).map(func1).value
//res.map(_.flatMap(identity))

//def mergeErrors[A, B] : (Either[A, Either[A, B]] => Either[A, B])
//    = (x:Either[A, Either[A, B]]) =>
//                                      x match {
//                                        case Left(x) => x.asLeft[B]
//                                        case Right(either) =>
//                                          either match {
//                                            case Left(x) => x.asLeft[B]
//                                            case Right(y) => y.asRight[A]
//                                          }
//                                      }







//val list = List(1,2,3,4,5,6,7,8)
//
//def filterOne:(List[Int] => Either[List[String], Int]) = (list:List[Int]) =>
//  list.collectFirst({case i if i == 3 => i}) match {
//    case Some(x) => x.asRight[List[String]]
//    case _ => List("User not found").asLeft[Int]
//  }




//import scala.concurrent.Future
//
//def eitherLeft() = 21.asRight[String]
//def eitherRight() = "error1".asLeft[Int]
//
//eitherLeft().map(_+ 20).toValidated.leftMap(_.reverse)
//eitherRight().map(_+ 20).toValidated.leftMap(_.reverse).toEither
//
//
//type ex[A] = Either[List[String], A]
//
//
//type FutureEither[A] = EitherT[Future, List[String], A]
//val y:Future[Either[List[String], Int]] = 10.pure[FutureEither].value
