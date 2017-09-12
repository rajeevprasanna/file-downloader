import cats._, cats.data._, cats.implicits._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

case class User(id:Long, name:String)

sealed trait Error
object Error {
  final case class UserNotFound(userId:Long) extends Error
  final case class ConnectionError(message:String) extends Error
}

import cats._, cats.data._, cats.implicits._
object UserRepo {

  def followers(userId: Long): EitherT[Future, Error, List[User]] =

    userId match {

      case 0L =>
        EitherT.right(Future { List(User(1, "Michael")) })

      case 1L =>
        EitherT.right(Future { List(User(0, "Vito")) })

      case x =>
        println("not found")
        EitherT.left(Future.successful { Error.UserNotFound(x) })
    }
}

import UserRepo.followers
def isFriend3(user1:Long, user2:Long):EitherT[Future, Error, Boolean] =
  for {
    a <- followers(user1)
    b <- followers(user2)
  } yield a.exists(_.id == user2) && b.exists(_.id == user1)




//import UserRepo.followers

//def isFriend0(user1:Long, user2:Long):Either[Error, Boolean] =
//  for {
//    a <- followers(user1).right
//    b <- followers(user2).right
//  } yield a.exists(_.id == user2) && b.exists(_.id == user1)
















