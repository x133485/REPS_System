package services

import models._
import java.time.{Instant, LocalDateTime, ZoneId}

/**
 * Service for managing energy storage in the plant
 */
object StorageManager {
  // Storage configuration - adjusted for more realistic consumption
  val MaxCapacity: Double = 10000000.0 // 10 GWh capacity (in MWh)
  val MinSafeLevel: Double = MaxCapacity * 0.15 // 15% of capacity
  val MaxSafeLevel: Double = MaxCapacity * 0.85 // 85% of capacity
  val DefaultConsumptionRate: Double = 20000.0 // 20,000 MWh consumed per hour - realistic city
  
  /**
   * Calculate new storage level based on energy production
   * Uses immutable approach - returns new value rather than modifying
   * 
   * @param currentLevel Current storage level
   * @param energyData List of energy data with production values
   * @param hoursPassed Hours passed since last update
   * @return New storage level
   */
  def updateStorage(
    currentLevel: Double, 
    energyData: List[EnergyData], 
    hoursPassed: Double = 1.0
  ): Double = {
    // Group data by source to avoid double-counting
    val dataBySource = energyData.groupBy(_.source)
    
    // For each source, take the average output over the period
    val avgOutputBySource = dataBySource.map { case (source, sourceData) =>
      val avgOutput = if (sourceData.nonEmpty) sourceData.map(_.energyOutput).sum / sourceData.length else 0.0
      (source, avgOutput * hoursPassed) // Total energy in MWh = average power (MW) * time (h)
    }
    
    // Sum all source productions
    val totalProduction = avgOutputBySource.values.sum
    
    // Calculate consumption based on time passed
    val consumption = DefaultConsumptionRate * hoursPassed
    
    // Calculate new level (bounded by 0 and MaxCapacity)
    val newLevel = (currentLevel + totalProduction - consumption).max(0.0).min(MaxCapacity)
    
    newLevel
  }
  
  /**
   * Get storage status based on current level
   * 
   * @param level Current storage level
   * @return Status string (Critical, Low, Normal, High)
   */
  def getStorageStatus(level: Double): String = {
    val percentage = (level / MaxCapacity) * 100
    
    if (percentage < 10) "Critical"
    else if (percentage < 30) "Low"
    else if (percentage > 90) "High"
    else "Normal"
  }
  
  /**
   * Check if storage needs attention (alerts)
   */
  def checkStorageAlerts(level: Double): Option[AlertSystem.Alert] = {
    if (level < MinSafeLevel) {
      Some(AlertSystem.Alert(
        AlertSystem.LowOutputAlert,
        Solar, // Just using Solar as a placeholder source
        s"Storage level critically low: ${(level/1000).toInt} GWh (${(level/MaxCapacity*100).toInt}%)"
      ))
    } else if (level > MaxSafeLevel) {
      Some(AlertSystem.Alert(
        AlertSystem.HighOutputAlert,
        Solar, // Just using Solar as a placeholder source
        s"Storage level critically high: ${(level/1000).toInt} GWh (${(level/MaxCapacity*100).toInt}%)"
      ))
    } else {
      None
    }
  }
  
  /**
   * Calculate how long current storage would last with no production
   */
  def calculateRemainingHours(level: Double, consumptionRate: Double = DefaultConsumptionRate): Double = {
    if (consumptionRate <= 0) Double.PositiveInfinity
    else level / consumptionRate
  }
  
  /**
   * Simulate storage impact of toggling energy sources
   */
  def simulateStorageImpact(
    currentLevel: Double,
    solarOn: Boolean,
    windOn: Boolean,
    hydroOn: Boolean,
    hoursToProject: Int = 24
  ): (Double, String) = {
    // More realistic production values for a city-scale system
    val solarProduction = if (solarOn) 400.0 * hoursToProject else 0.0  // 400 MW average
    val windProduction = if (windOn) 2000.0 * hoursToProject else 0.0   // 2000 MW average 
    val hydroProduction = if (hydroOn) 1800.0 * hoursToProject else 0.0 // 1800 MW average
    
    val consumption = DefaultConsumptionRate * hoursToProject
    val production = solarProduction + windProduction + hydroProduction
    val projectedLevel = (currentLevel + production - consumption).max(0.0).min(MaxCapacity)
    
    val impact = if (projectedLevel > currentLevel) "increase" else "decrease"
    val message = f"Projected ${hoursToProject}h impact: ${impact} to ${projectedLevel/1000}%.1f GWh (${(projectedLevel/MaxCapacity*100).toInt}%%)"
    
    (projectedLevel, message)
  }
}