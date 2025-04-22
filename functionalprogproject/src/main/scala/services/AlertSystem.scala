package services

import models._
import java.time.{Instant, LocalDateTime, ZoneId}

object AlertSystem {
  // Define alert types
  sealed trait AlertType
  case object LowOutputAlert extends AlertType
  case object HighOutputAlert extends AlertType
  case object EquipmentMalfunctionAlert extends AlertType
  
  case class Alert(
    alertType: AlertType,
    source: EnergySource,
    message: String,
    timestamp: Long = Instant.now.getEpochSecond
  )
  
  // Detect issues in the energy output data
  def detectIssues(data: List[EnergyData]): List[Alert] = {
    val lowOutputAlerts = detectLowOutput(data)
    val highOutputAlerts = detectHighOutput(data)
    val malfunctionAlerts = detectMalfunction(data)
    
    lowOutputAlerts ++ highOutputAlerts ++ malfunctionAlerts
  }
  
  private def detectLowOutput(data: List[EnergyData]): List[Alert] = {
    data.filter(_.status == "Low").map { d =>
      Alert(
        LowOutputAlert,
        d.source,
        s"Low energy output detected from ${d.source} at ${d.location}: ${d.energyOutput} units"
      )
    }
  }
  
  private def detectHighOutput(data: List[EnergyData]): List[Alert] = {
    data.filter(_.status == "High").map { d =>
      Alert(
        HighOutputAlert,
        d.source,
        s"Unusually high energy output from ${d.source} at ${d.location}: ${d.energyOutput} units"
      )
    }
  }
  
  private def detectMalfunction(data: List[EnergyData]): List[Alert] = {
    // For this example, we'll consider a malfunction when output is 0
    data.filter(_.energyOutput == 0.0).map { d =>
      Alert(
        EquipmentMalfunctionAlert,
        d.source,
        s"Possible equipment malfunction at ${d.location}: No energy output detected"
      )
    }
  }
}