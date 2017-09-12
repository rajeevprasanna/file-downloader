package temp

/**
  * Created by rajeevprasanna on 7/11/17.
  */
import cats.data.Validated
import cats.instances.all._
import cats.syntax.cartesian._
import cats.syntax.either._


object userValidation extends App {

  case class User(name:String, age:Int)

  type FormData = Map[String, String]
  type ErrorsOr[A] = Either[List[String], A]
  type AllErrorsOr[A] = Validated[List[String], A]

  def getValue(name:String)(data: FormData):ErrorsOr[String] = data.get(name).toRight(List(s"$name field not specified"))

  type NumFmtExn = NumberFormatException
  def parseInt(name:String)(data:String):ErrorsOr[Int] =
          Right(data).flatMap(s => Either.catchOnly[NumFmtExn](s.toInt)).
            leftMap(_ => List(s"$name must be an integer"))

  def nonBlank(name: String)(data: String): ErrorsOr[String] = data.asRight.filterOrElse(_.nonEmpty, List(s"$name must not be empty"))
  def nonNegative(name: String)(data: Int): ErrorsOr[Int] = data.asRight.filterOrElse(_>0, List(s"$name must be non negative"))


//We use flatMap to combine the rules sequentially:
  def readName(data:FormData):ErrorsOr[String] = getValue("name")(data).flatMap(nonBlank("name"))
  def readAge(data:FormData):ErrorsOr[Int] = getValue("age")(data)
                                                .flatMap(nonBlank("age"))
                                                .flatMap(parseInt("age"))
                                                .flatMap(nonNegative("age"))

  def readUser(data:FormData):AllErrorsOr[User] =  (readName(data).toValidated |@| readAge(data).toValidated).map(User.apply)

  val x = readUser(Map("name" -> "Dave", "age" -> "37"))
  val y = readUser(Map("age" -> "-1"))

  println(x)
  println(y)
}
