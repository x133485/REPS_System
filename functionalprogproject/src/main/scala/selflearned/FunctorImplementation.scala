package selflearned

/**
 * Implementation of Functor concept in Scala
 * 
 * A Functor is a type class that abstracts over a type constructor that 
 * represents a "computational context". It provides a `map` function that 
 * allows transforming values inside this context without affecting the context itself.
 */
object FunctorImplementation {
  
  // Functor type class definition
  trait Functor[F[_]] {
    def map[A, B](fa: F[A])(f: A => B): F[B]
  }
  
  // Companion object with functor instances
  object Functor {
    // List Functor instance
    implicit val listFunctor: Functor[List] = new Functor[List] {
      def map[A, B](fa: List[A])(f: A => B): List[B] = fa.map(f)
    }
    
    // Option Functor instance
    implicit val optionFunctor: Functor[Option] = new Functor[Option] {
      def map[A, B](fa: Option[A])(f: A => B): Option[B] = fa.map(f)
    }
    
    // Example: Custom container with Functor instance
    case class Box[A](value: A)
    
    implicit val boxFunctor: Functor[Box] = new Functor[Box] {
      def map[A, B](fa: Box[A])(f: A => B): Box[B] = Box(f(fa.value))
    }
  }
  
  // Syntax extension for Functor
  implicit class FunctorOps[F[_], A](fa: F[A])(implicit F: Functor[F]) {
    def fmap[B](f: A => B): F[B] = F.map(fa)(f)
  }
  
  // Demonstration of using Functors
  def main(args: Array[String]): Unit = {
    import Functor._
    
    // Using List functor
    val numbers = List(1, 2, 3, 4, 5)
    val doubled = numbers.fmap(_ * 2)
    println(s"Original list: $numbers")
    println(s"Doubled list: $doubled")
    
    // Using Option functor
    val someValue = Option(10)
    val noneValue = Option.empty[Int]
    println(s"Some value mapped: ${someValue.fmap(_ + 5)}")
    println(s"None value mapped: ${noneValue.fmap(_ + 5)}")
    
    // Using custom Box functor
    val box = Box(100)
    val squaredBox = box.fmap(x => x * x)
    println(s"Original box: $box")
    println(s"Squared box: $squaredBox")
  }
}