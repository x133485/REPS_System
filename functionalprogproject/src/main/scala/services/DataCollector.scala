package services

import models._
import java.time.{Instant, LocalDateTime, ZoneId}

object DataCollector {
  // Immutable approach to collecting data
  def collectData(source: EnergySource, output: Double, customTimestamp: Option[Long] = None): EnergyData = {
    EnergyData(
      timestamp = customTimestamp.getOrElse(Instant.now.getEpochSecond),
      source = source,
      energyOutput = output,
      location = getLocation(source),
      status = determineStatus(output, source)
    )
  }
  
  private def getLocation(source: EnergySource): String = source match {
    case Solar => "Solar Array A"
    case Wind => "Turbine Field B"
    case Hydro => "Dam C"
  }
  
  private def determineStatus(output: Double, source: EnergySource): String = {
    // Determine status based on output level and expected range for source
    val thresholds = source match {
      case Solar => (5.0, 15.0) // Min, Max expected output
      case Wind => (3.0, 25.0)
      case Hydro => (10.0, 50.0)
    }
    
    if (output < thresholds._1) "Low"
    else if (output > thresholds._2) "High"
    else "Normal"
  }
}