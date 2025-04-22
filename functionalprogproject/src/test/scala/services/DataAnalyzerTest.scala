package services

import models._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DataAnalyzerTest extends AnyFlatSpec with Matchers {
  
  "DataAnalyzer" should "calculate mean correctly" in {
    val data = List(1.0, 2.0, 3.0, 4.0, 5.0)
    DataAnalyzer.calculateMean(data) shouldBe Some(3.0)
  }
  
  it should "return None for mean of empty list" in {
    DataAnalyzer.calculateMean(List.empty) shouldBe None
  }
  
  it should "calculate median correctly for odd number of elements" in {
    val data = List(1.0, 3.0, 5.0, 7.0, 9.0)
    DataAnalyzer.calculateMedian(data) shouldBe Some(5.0)
  }
  
  it should "calculate median correctly for even number of elements" in {
    val data = List(1.0, 3.0, 5.0, 7.0)
    DataAnalyzer.calculateMedian(data) shouldBe Some(4.0)
  }
  
  // Add more tests for other functions
}