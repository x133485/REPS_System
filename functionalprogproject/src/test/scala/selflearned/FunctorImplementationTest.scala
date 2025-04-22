package selflearned

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import selflearned.FunctorImplementation._
import selflearned.FunctorImplementation.Functor._

class FunctorImplementationTest extends AnyFlatSpec with Matchers {
  
  "List Functor" should "map over a list correctly" in {
    val numbers = List(1, 2, 3)
    numbers.fmap(_ * 2) shouldBe List(2, 4, 6)
  }
  
  "Option Functor" should "map over Some correctly" in {
    val someValue = Option(5)
    someValue.fmap(_ + 10) shouldBe Some(15)
  }
  
  it should "map over None correctly" in {
    val noneValue = Option.empty[Int]
    noneValue.fmap(_ + 10) shouldBe None
  }
  
  "Box Functor" should "map over Box values correctly" in {
    val box = Box(42)
    box.fmap(_ * 2) shouldBe Box(84)
  }
}