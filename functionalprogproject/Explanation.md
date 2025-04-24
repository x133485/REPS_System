# Renewable Energy Plant System (REPS) Project Explanation

## Overview

In this project, I created a Renewable Energy Plant System (REPS) that manages and monitors renewable energy production from different sources. The system follows functional programming principles, using immutable data structures, pure functions, and higher-order functions throughout the codebase.

## Project Requirements Implementation

### 1. Monitoring and Controlling Renewable Energy Sources

I implemented monitoring and control functionality for three renewable energy sources:
- **Solar panels**
- **Wind turbines**
- **Hydropower stations**

The system allows toggling each energy source on/off and seeing how this affects the overall energy production. This is mainly handled in `REPSMain.scala` where the `handlePlantControl` function lets users toggle different sources and simulate their impact.

```scala
def handlePlantControl(state: AppState): AppState = {
  // Toggle controls for solar, wind, and hydro energy sources
  // Shows real-time impacts on storage levels
}
```

### 2. Collecting and Storing Energy Data

The system collects data from renewable sources through:

1. **FingridClient**: This service fetches real energy data from the Fingrid API, giving realistic values for energy production in Finland.

2. **DataStorage**: This service saves collected data to files and loads it back:
   ```scala
   def saveDataListToFile(data: List[EnergyData], filename: String): Either[String, Unit]
   def loadAllDataFromFile(filename: String): Either[String, List[EnergyData]]
   ```

The data is stored as `EnergyData` objects containing:
- Timestamp
- Energy source (Solar/Wind/Hydro)
- Energy output value
- Location
- Status (Low/Normal/High)

### 3. Energy Generation and Storage View

I implemented a comprehensive view of the plant's energy generation and storage:

1. **ConsoleUI.displayEnergyData**: Shows collected energy data with pagination
   ```scala
   def displayEnergyData(data: List[EnergyData]): Unit
   ```

2. **StorageManager**: Tracks storage levels and provides projections
   ```scala
   def updateStorage(currentLevel: Double, energyData: List[EnergyData], hoursPassed: Double): Double
   def simulateStorageImpact(currentLevel: Double, solarOn: Boolean, windOn: Boolean, hydroOn: Boolean): (Double, String)
   ```

3. The system shows:
   - Current storage level in GWh
   - Storage status (Critical/Low/Normal/High)
   - Projected hours until depletion
   - Impact of toggling different energy sources

### 4. Data Analysis Capabilities

The `DataAnalyzer` service provides comprehensive analysis including:

1. **Statistical functions**:
   ```scala
   def calculateMean(data: List[Double]): Option[Double]
   def calculateMedian(data: List[Double]): Option[Double]
   def calculateMode(data: List[Double]): Option[Double]
   def calculateRange(data: List[Double]): Option[Double]
   def calculateMidrange(data: List[Double]): Option[Double]
   ```

2. **Filtering options**:
   - By hour: `filterByHour(data: List[EnergyData], hour: Int)`
   - By day: `filterByDay(data: List[EnergyData], day: Int)`
   - By week: `filterByWeek(data: List[EnergyData], weekOfYear: Int)`
   - By month: `filterByMonth(data: List[EnergyData], month: Int)`

3. **Search functionality**:
   - By energy output range: `searchByEnergyOutput(data: List[EnergyData], min: Double, max: Double)`
   - By status: `searchByStatus(data: List[EnergyData], status: String)`

4. **Data transformation using functors**:
   - Scale values: `scaleEnergyOutputs(data: List[EnergyData], factor: Double)`
   - Normalize values: `normalizeValues(data: List[EnergyData])`
   - Apply thresholds: `applyThresholds(data: List[EnergyData], minValue: Option[Double], maxValue: Option[Double])`

### 5. Issue Detection and Alerts

The `AlertSystem` detects problems with renewable energy sources:

1. **Alert types**:
   - Low output alerts
   - High output alerts
   - Equipment malfunction alerts

2. **Detection logic**:
   ```scala
   def detectIssues(data: List[EnergyData]): List[Alert]
   private def detectLowOutput(data: List[EnergyData]): List[Alert]
   private def detectHighOutput(data: List[EnergyData]): List[Alert]
   private def detectMalfunction(data: List[EnergyData]): List[Alert]
   ```

3. The system generates alerts when:
   - Energy output is consistently low (>75% of readings)
   - Energy output is unusually high
   - Equipment malfunction is detected (zero output)

## Functional Programming Implementation

Throughout the project, I followed functional programming principles:

### Immutability

All data structures are immutable. For example, the application state is managed through an immutable case class:

```scala
case class AppState(
  storageLevel: Double = 5000000.0,  // 5 GWh (50% of capacity)
  solarOn: Boolean = true,
  windOn: Boolean = true,
  hydroOn: Boolean = true,
  data: List[EnergyData] = Nil
)
```

When state changes, a new instance is created rather than modifying the existing one.

### Pure Functions

Functions avoid side effects and always produce the same output for the same input. For example:

```scala
def calculateMean(data: List[Double]): Option[Double] = {
  @scala.annotation.tailrec
  def sumRec(values: List[Double], acc: Double): Double = values match {
    case Nil => acc
    case head :: tail => sumRec(tail, acc + head)
  }
  
  if (data.isEmpty) None
  else Some(sumRec(data, 0.0) / data.length)
}
```

### Recursion Instead of Loops

Iterative operations use recursion instead of loops, like in this recursive summation function:

```scala
@scala.annotation.tailrec
def sumRec(values: List[Double], acc: Double): Double = values match {
  case Nil => acc
  case head :: tail => sumRec(tail, acc + head)
}
```

### Higher-Order Functions

The system extensively uses higher-order functions that take functions as parameters:

```scala
def transformEnergyOutput[F[_]](data: F[EnergyData])(transformation: Double => Double)
  (implicit F: Functor[F]): F[EnergyData] = {
  data.fmap(energyData => 
    energyData.copy(energyOutput = transformation(energyData.energyOutput))
  )
}
```

## Self-Learned Topic: Functors

For Part II of the project, I implemented and explored the concept of Functors:

### What is a Functor?

A Functor is a type class that represents a computational context with a mapping operation. It allows transforming values inside a context without affecting the context itself.

The core of a functor is the `map` function with this signature:
```scala
def map[A, B](fa: F[A])(f: A => B): F[B]
```

### Implementation

I created a `Functor` trait with implementations for common types:

```scala
trait Functor[F[_]] {
  def map[A, B](fa: F[A])(f: A => B): F[B]
}

object Functor {
  // List Functor instance
  implicit val listFunctor: Functor[List] = new Functor[List] {
    def map[A, B](fa: List[A])(f: A => B): List[B] = fa.map(f)
  }
  
  // Option Functor instance
  implicit val optionFunctor: Functor[Option] = new Functor[Option] {
    def map[A, B](fa: Option[A])(f: A => B): Option[B] = fa.map(f)
  }
  
  // Custom Box Functor
  case class Box[A](value: A)
  implicit val boxFunctor: Functor[Box] = new Functor[Box] {
    def map[A, B](fa: Box[A])(f: A => B): Box[B] = Box(f(fa.value))
  }
}
```

### Practical Application in REPS

I applied the Functor concept to transform energy data consistently across different contexts:

```scala
def transformEnergyOutput[F[_]](data: F[EnergyData])(transformation: Double => Double)
  (implicit F: Functor[F]): F[EnergyData] = {
  data.fmap(energyData => 
    energyData.copy(energyOutput = transformation(energyData.energyOutput))
  )
}
```

This allowed operations like:
1. Scaling energy values: `scaleEnergyOutput[F[_]](data: F[EnergyData])(scaleFactor: Double)`
2. Converting units: `convertUnits[F[_]](data: F[EnergyData])(fromUnit: String, toUnit: String)`
3. Applying thresholds: `applyThreshold[F[_]](data: F[EnergyData])(min: Option[Double], max: Option[Double])`

The power of functors is that these operations work consistently regardless of the context (List, Option, etc.) that contains the energy data.
