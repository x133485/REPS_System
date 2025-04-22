package services

import models._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AlertSystemTest extends AnyFlatSpec with Matchers {
  
  "AlertSystem" should "detect low output issues" in {
    val data = List(
      EnergyData(1617289200, Solar, 3.0, "Solar Array A", "Low"),
      EnergyData(1617289200, Wind, 10.0, "Turbine Field B", "Normal")
    )
    
    val alerts = AlertSystem.detectIssues(data)
    alerts.length shouldBe 1
    alerts.head.alertType shouldBe AlertSystem.LowOutputAlert
    alerts.head.source shouldBe Solar
  }
  
  // Add more tests for high output and malfunction
}