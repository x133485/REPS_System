package models

sealed trait EnergySource
case object Solar extends EnergySource
case object Wind extends EnergySource
case object Hydro extends EnergySource

case class EnergyData(
  timestamp: Long,
  source: EnergySource,
  energyOutput: Double,
  location: String,
  status: String
)