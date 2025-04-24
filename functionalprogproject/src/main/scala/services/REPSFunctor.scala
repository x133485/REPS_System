package services

import models._
import selflearned.FunctorImplementation.{Functor, FunctorOps}

/**
 * Integrates Functor concept with REPS domain models
 * Shows how functors can be used for consistent data transformations
 */
object REPSFunctor {
  // Import the functor type class and instances
  import selflearned.FunctorImplementation.Functor._
  
  /**
   * Transform energy output values within a collection of EnergyData
   * Demonstrates functor pattern for transforming data within a context
   * 
   * @param data Collection of energy data wrapped in a context with a Functor instance
   * @param transformation Function to apply to energy output values
   * @return Collection with transformed energy data
   */
  def transformEnergyOutput[F[_]](data: F[EnergyData])(transformation: Double => Double)
    (implicit F: Functor[F]): F[EnergyData] = {
    
    data.fmap(energyData => 
      energyData.copy(energyOutput = transformation(energyData.energyOutput))
    )
  }
  
  /**
   * Apply scale factor to energy values in a collection
   */
  def scaleEnergyOutput[F[_]](data: F[EnergyData])(scaleFactor: Double)
    (implicit F: Functor[F]): F[EnergyData] = {
    
    transformEnergyOutput(data)(_ * scaleFactor)
  }
  
  /**
   * Apply unit conversion to energy values
   * For example, convert from MW to GW
   */
  def convertUnits[F[_]](data: F[EnergyData])(fromUnit: String, toUnit: String)
    (implicit F: Functor[F]): F[EnergyData] = {
    
    val conversionFactor = (fromUnit, toUnit) match {
      case ("MW", "GW") => 0.001 // MW to GW
      case ("MW", "kW") => 1000.0 // MW to kW
      case ("GW", "MW") => 1000.0 // GW to MW
      case ("kW", "MW") => 0.001 // kW to MW
      case _ => 1.0 // No conversion or unsupported
    }
    
    scaleEnergyOutput(data)(conversionFactor)
  }
  
  /**
   * Apply threshold to energy values
   * For example, set minimum or maximum values
   */
  def applyThreshold[F[_]](data: F[EnergyData])(min: Option[Double] = None, max: Option[Double] = None)
    (implicit F: Functor[F]): F[EnergyData] = {
    
    transformEnergyOutput(data) { value =>
      val withMin = min.map(m => math.max(value, m)).getOrElse(value)
      max.map(m => math.min(withMin, m)).getOrElse(withMin)
    }
  }
}