
val l = List(1,2,3,4)

l.reduceLeft(_+_)

List[Int](1, 2).mkString(", ")//foldLeft("")(_ +", "+ _)
List[Int]().mkString(", ")