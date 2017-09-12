import cats.Functor
import cats.instances.all._
import cats.data._
import cats.syntax.all._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

val func = (x:Int) => x+1
val f2 = Functor[Option].lift(func)


val funcx = (x:Int) => Future.successful(x)
val f3 = Functor[Future].lift(funcx).flatten

f2(Some(10))

10.pure[Future]