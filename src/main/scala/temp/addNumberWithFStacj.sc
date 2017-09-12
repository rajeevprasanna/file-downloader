import scala.concurrent.Future
import cats._
import cats.data._
import cats.implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

type Logged[A] = Writer[List[String], A]

def parseNumber(str:String):Logged[Option[Int]] =
  Try(str.toInt).toOption match {
    case Some(x) => Writer(List(s"Read $str"), x.some)
    case None =>Writer(List(s"Failed on $str"), none)
  }

def addNumbers(a:String, b:String, c:String):Logged[Option[Int]] =
{
  val result =
    for {
      a <- OptionT(parseNumber(a))
      b <- OptionT(parseNumber(b))
      c <- OptionT(parseNumber(c))
    } yield a + b + c
  result.value
}

val result1 = addNumbers("1", "2", "3")


