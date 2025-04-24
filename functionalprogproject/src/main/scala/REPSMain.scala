import models._
import services._
import ui._
import scala.io.StdIn
import scala.util.{Try, Success, Failure}
import java.time.{Instant, LocalDateTime, ZoneId}

/**
 * Main application for the Renewable Energy Plant System
 * Implemented using functional programming principles:
 * - Immutable data structures for state
 * - Pure functions with no side effects
 * - Recursion instead of loops
 * - Pattern matching for control flow
 */
object REPSMain extends App {  
  /**
   * Case class to hold application state immutably
   * Any state change creates a new instance
   */
  case class AppState(
    storageLevel: Double = 5000000.0,  // 5 GWh (50% of capacity)
    solarOn: Boolean = true,
    windOn: Boolean = true,
    hydroOn: Boolean = true,
    data: List[EnergyData] = Nil
  )

  /**
   * Main application entry point
   */
  def runApplication(): Unit = {
    println("Starting Renewable Energy Plant System...")
    // Initial state
    val initialState = AppState()
    // Start the main application loop with the initial state
    mainLoop(initialState)
  }
  
  // Update the mainLoop method
  def mainLoop(state: AppState): Unit = {
    // Display menu
    ConsoleUI.displayMenu()
    
    def readValidChoice(): Unit = {
      StdIn.readLine() match {
        case "1" => // Data collection 
          val newState = handleDataCollection(state)
          mainLoop(newState)
          
        case "2" => // View energy data
          handleViewData(state)
          mainLoop(state) // State unchanged
          
        case "3" => // Analyze energy data
          val newState = handleDataAnalysis(state)
          mainLoop(newState)
          
        case "4" => // Check alerts
          handleAlerts(state)
          mainLoop(state) // State unchanged
          
        case "5" => // View data from file
          val newState = handleLoadData(state)
          mainLoop(newState)
          
        case "6" => // Control plant operation
          val newState = handlePlantControl(state)
          mainLoop(newState)
          
        case "7" => // Save data to file
          handleSaveData(state)
          mainLoop(state) // State unchanged
          
        case "8" => // Exit
          println("Exiting Renewable Energy Plant System. Goodbye!")
          // End recursion
          
        case _ => // Invalid choice
          println("Invalid choice. Please try again.")
          print("Enter your choice: ")
          readValidChoice()
      }
    }
    
    readValidChoice()
  }
  
  /**
   * Handle data collection menu
   * Returns updated state with new data and storage level
   */
  def handleDataCollection(state: AppState): AppState = {
    def handleTimeChoice(timeChoice: String, currentState: AppState): AppState = {
      timeChoice match {
        case "1" =>
          println("Fetching data for the last hour...")
          val newData = DataManager.fetchLastHoursData(1, currentState.solarOn, currentState.windOn, currentState.hydroOn)
          ConsoleUI.displayEnergyData(newData)
          updateStateWithData(currentState, newData)
          
        case "2" =>
          println("Fetching data for the last 24 hours...")
          val newData = DataManager.fetchLastHoursData(24, currentState.solarOn, currentState.windOn, currentState.hydroOn)
          ConsoleUI.displayEnergyData(newData)
          updateStateWithData(currentState, newData)
          
        case "3" =>
          println("Fetching data for the last 7 days...")
          val now = Instant.now().getEpochSecond
          val sevenDaysAgo = now - (7 * 24 * 3600)
          val newData = DataManager.fetchAllData(sevenDaysAgo, now, currentState.solarOn, currentState.windOn, currentState.hydroOn)
          ConsoleUI.displayEnergyData(newData)
          updateStateWithData(currentState, newData)
          
        case "4" =>
          ConsoleUI.promptForDateRange() match {
            case Some((startTime, endTime)) =>
              println("Fetching data for the specified date range...")
              val newData = DataManager.fetchAllData(startTime, endTime, currentState.solarOn, currentState.windOn, currentState.hydroOn)
              ConsoleUI.displayEnergyData(newData)
              updateStateWithData(currentState, newData)
            case None =>
              println("Operation cancelled.")
              currentState
          }
          
        case "5" =>
          currentState // Return unchanged state
          
        case _ =>
          println("Invalid choice. Please try again.")
          print("Enter your choice: ")
          val newChoice = StdIn.readLine()
          handleTimeChoice(newChoice, currentState)
      }
    }
    
    def promptForTimeChoice(currentState: AppState): AppState = {
      println("\n===== COLLECT REAL-TIME ENERGY DATA =====")
      println("1. Last hour")
      println("2. Last 24 hours")
      println("3. Last 7 days")
      println("4. Custom date range")
      println("5. Back to Main Menu")
      print("Enter your choice: ")
      
      val choice = StdIn.readLine()
      handleTimeChoice(choice, currentState)
    }
    
    // Helper function to update state with new data
    def updateStateWithData(currentState: AppState, newData: List[EnergyData]): AppState = {
      if (newData.isEmpty) {
        currentState
      } else {
        // Calculate how many hours the data represents
        val dataHours = (newData.map(_.timestamp).max - newData.map(_.timestamp).min) / 3600.0
        val hoursToUse = if (dataHours > 0) dataHours else 1.0
        
        // Calculate new storage level
        val newStorageLevel = StorageManager.updateStorage(
          currentState.storageLevel, 
          newData, 
          hoursToUse
        )
        
        println(f"Storage updated to: ${newStorageLevel/1000}%.1f GWh")
        
        // Return updated state
        currentState.copy(data = newData, storageLevel = newStorageLevel)
      }
    }
    
    promptForTimeChoice(state)
  }
  
  /**
   * Handle viewing energy data
   */
  def handleViewData(state: AppState): Unit = {
    if (state.data.isEmpty) {
      println("\n===== ENERGY DATA =====")
      println("No data available to view. Please collect or load data first.")
    } else {
      ConsoleUI.displayEnergyData(state.data)
    }
  }
  
  /**
   * Handle data analysis menu
   * Returns potentially updated state
   */
  def handleDataAnalysis(state: AppState): AppState = {
    if (state.data.isEmpty) {
      println("\n===== DATA ANALYSIS =====")
      println("No data available to analyze. Please collect or load data first.")
      return state
    }
    
    def analyzeData(currentState: AppState): AppState = {
      // Display menu once
      ConsoleUI.displayAnalysisMenu()
      
      // Loop until valid input is provided
      def readValidChoice(): AppState = {
        StdIn.readLine() match {
          case "1" =>
            ConsoleUI.displayStatistics(currentState.data)
            analyzeData(currentState)
            
          case "2" =>
            ConsoleUI.promptForDateRange() match {
              case Some((startTime, endTime)) =>
                val filteredData = currentState.data.filter(d => 
                  d.timestamp >= startTime && d.timestamp <= endTime)
                ConsoleUI.displayEnergyData(filteredData)
              case None => // Do nothing
            }
            analyzeData(currentState)
            
          case "3" =>
            ConsoleUI.promptForHour() match {
              case Some(hour) =>
                val filteredData = DataAnalyzer.filterByHour(currentState.data, hour)
                ConsoleUI.displayEnergyData(filteredData)
              case None => // Do nothing
            }
            analyzeData(currentState)
            
          case "4" =>
            ConsoleUI.promptForDay() match {
              case Some((year, month, day)) =>
                println(s"Fetching data for $year-$month-$day...")
                val dayData = DataManager.fetchDataForDay(
                  year, month, day, 
                  currentState.solarOn, currentState.windOn, currentState.hydroOn
                )
                ConsoleUI.displayEnergyData(dayData)
              case None => // Do nothing
            }
            analyzeData(currentState)
            
          case "5" =>
            ConsoleUI.promptForWeek() match {
              case Some((year, week)) =>
                println(s"Fetching data for week $week of $year...")
                val weekData = DataManager.fetchDataForWeek(
                  year, week, 
                  currentState.solarOn, currentState.windOn, currentState.hydroOn
                )
                ConsoleUI.displayEnergyData(weekData)
              case None => // Do nothing
            }
            analyzeData(currentState)
            
          case "6" =>
            ConsoleUI.promptForMonth() match {
              case Some((year, month)) =>
                println(s"Fetching data for $year-$month...")
                val monthData = DataManager.fetchDataForMonth(
                  year, month, 
                  currentState.solarOn, currentState.windOn, currentState.hydroOn
                )
                ConsoleUI.displayEnergyData(monthData)
              case None => // Do nothing
            }
            analyzeData(currentState)
            
          case "7" =>
            println("Enter status to search (e.g., Low, Normal, High):")
            val status = StdIn.readLine()
            val filteredData = DataAnalyzer.searchByStatus(currentState.data, status)
            ConsoleUI.displayEnergyData(filteredData)
            analyzeData(currentState)
            
          case "8" =>
            currentState // Exit analysis menu
            
          case "9" =>
            handleFunctorOperations(currentState.data)
            analyzeData(currentState)
            
          case _ =>
            println("Invalid choice. Please try again.")
            print("Enter your choice: ")
            readValidChoice()
        }
      }
      
      readValidChoice()
    }
    
    analyzeData(state)
  }
  
  /**
   * Handle alert checking
   */
  def handleAlerts(state: AppState): Unit = {
    if (state.data.isEmpty) {
      println("\n===== SYSTEM ALERTS =====")
      println("No data available to check alerts. Please collect or load data first.")
    } else {
      val dataAlerts = AlertSystem.detectIssues(state.data)
      val storageAlert = StorageManager.checkStorageAlerts(state.storageLevel)
      
      val allAlerts = storageAlert match {
        case Some(alert) => alert :: dataAlerts
        case None => dataAlerts
      }
      
      ConsoleUI.displayAlerts(allAlerts)
    }
  }
  
  /**
   * Handle loading data from file
   * Returns updated state with new data
   */
  def handleLoadData(state: AppState): AppState = {
    println("Enter filename to load:")
    val filename = StdIn.readLine().trim
    
    DataStorage.loadAllDataFromFile(filename) match {
      case Left(errorMsg) =>
        println(errorMsg)
        state
      case Right(fileData) =>
        if (fileData.isEmpty) {
          println("No data available in the selected file.")
          state
        } else {
          ConsoleUI.displayEnergyData(fileData)
          state.copy(data = fileData)
        }
    }
  }
  
  /**
   * Handle plant control operations
   * Returns updated state with potentially changed source statuses and storage
   */
  def handlePlantControl(state: AppState): AppState = {
    def controlLoop(currentState: AppState): AppState = {
      // Get storage status
      val storageStatus = StorageManager.getStorageStatus(currentState.storageLevel)
      val hoursRemaining = StorageManager.calculateRemainingHours(currentState.storageLevel)
      val (projectedLevel, impactMessage) = StorageManager.simulateStorageImpact(
        currentState.storageLevel, 
        currentState.solarOn, 
        currentState.windOn, 
        currentState.hydroOn
      )
      
      println("\n===== CONTROL PLANT OPERATION =====")
      println(f"Storage Level: ${currentState.storageLevel/1000}%.1f / ${StorageManager.MaxCapacity/1000} GWh (${(currentState.storageLevel/StorageManager.MaxCapacity*100).toInt}%%)")
      println(s"Status: $storageStatus")
      println(f"Time until depletion at current rate: ${hoursRemaining.toInt}h ${(hoursRemaining % 1 * 60).toInt}m")
      println(s"$impactMessage")
      println(s"1. Adjust Storage Level")
      println(s"2. Toggle Solar Panels (Currently: ${if (currentState.solarOn) "ON" else "OFF"})")
      println(s"3. Toggle Hydro Plant (Currently: ${if (currentState.hydroOn) "ON" else "OFF"})")
      println(s"4. Toggle Wind Turbines (Currently: ${if (currentState.windOn) "ON" else "OFF"})")
      println("5. Simulate Storage Impact")
      println("6. Back to Main Menu")
      print("Enter your choice: ")
      
      def readValidChoice(): AppState = {
        StdIn.readLine() match {
          case "1" =>
            println("Enter new storage level in GWh (0-10000):")
            def readValidLevel(): AppState = {
              Try(StdIn.readLine().toDouble) match {
                case Success(level) if level >= 0 && level <= StorageManager.MaxCapacity/1000 =>
                  val newLevel = level * 1000 // Convert GWh input to MWh
                  println(f"Storage level set to ${level}%.1f GWh")
                  controlLoop(currentState.copy(storageLevel = newLevel))
                case _ =>
                  println(f"Invalid value. Please enter a number between 0 and ${StorageManager.MaxCapacity/1000}:")
                  readValidLevel()
              }
            }
            readValidLevel()
            
          case "2" =>
            val newSolarStatus = !currentState.solarOn
            println(s"Solar panels turned " + (if (newSolarStatus) "ON" else "OFF"))
            controlLoop(currentState.copy(solarOn = newSolarStatus))
            
          case "3" =>
            val newHydroStatus = !currentState.hydroOn
            println(s"Hydro plant turned " + (if (newHydroStatus) "ON" else "OFF"))
            controlLoop(currentState.copy(hydroOn = newHydroStatus))
            
          case "4" =>
            val newWindStatus = !currentState.windOn
            println(s"Wind turbines turned " + (if (newWindStatus) "ON" else "OFF"))
            controlLoop(currentState.copy(windOn = newWindStatus))
            
          case "5" =>
            println("Enter hours to simulate (1-168):")
            def readValidHours(): AppState = {
              Try(StdIn.readLine().toInt) match {
                case Success(hours) if hours >= 1 && hours <= 168 =>
                  val (projected, message) = StorageManager.simulateStorageImpact(
                    currentState.storageLevel, 
                    currentState.solarOn, 
                    currentState.windOn, 
                    currentState.hydroOn, 
                    hours
                  )
                  println(s"Simulation result: $message")
                  controlLoop(currentState)
                case _ =>
                  println("Invalid value. Please enter a number between 1 and 168:")
                  readValidHours()
              }
            }
            readValidHours()
            
          case "6" =>
            currentState // Return to main menu
            
          case _ =>
            println("Invalid choice. Please try again.")
            print("Enter your choice: ")
            readValidChoice()
        }
      }
      
      readValidChoice()
    }
    
    controlLoop(state)
  }
  
  /**
   * Handle saving data to file
   */
  def handleSaveData(state: AppState): Unit = {
    if (state.data.isEmpty) {
      println("No data to save. Please collect or view data first.")
    } else {
      def saveWithFilename(): Unit = {
        println("Enter filename to save (must end with .csv or .xls):")
        val filename = StdIn.readLine().trim
        
        DataStorage.saveDataListToFile(state.data, filename) match {
          case Right(_) =>
            println(s"Data saved to $filename")
          case Left(errorMsg) =>
            println(errorMsg)
            saveWithFilename()  // Try again recursively
        }
      }
      
      saveWithFilename()
    }
  }
  
  /**
   * Demonstrate Functor operations on energy data
   * Shows how functors enable consistent transformations across different contexts
   */
  def handleFunctorOperations(data: List[EnergyData]): Unit = {
    if (data.isEmpty) {
      println("\n===== FUNCTOR OPERATIONS =====")
      println("No data available for functor operations. Please collect data first.")
      return
    }

    println("\n===== FUNCTOR OPERATIONS =====")
    println("1. Scale energy outputs (multiply by factor)")
    println("2. Normalize values to 0-100 scale")
    println("3. Apply min/max thresholds")
    println("4. Back to Analysis Menu")
    print("Enter your choice: ")

    def readValidChoice(): Unit = {
      StdIn.readLine() match {
        case "1" =>
          println("Enter scaling factor:")
          def readValidFactor(): Unit = {
            Try(StdIn.readLine().toDouble) match {
              case Success(factor) =>
                val scaled = DataAnalyzer.scaleEnergyOutputs(data, factor)
                println(s"Scaled ${scaled.size} energy records by factor $factor")
                ConsoleUI.displayEnergyData(scaled)
              case Failure(_) =>
                println("Invalid factor. Please enter a valid number:")
                readValidFactor()
            }
          }
          readValidFactor()

        case "2" =>
          val normalized = DataAnalyzer.normalizeValues(data)
          println("Normalized energy values to 0-100 scale")
          ConsoleUI.displayEnergyData(normalized)

        case "3" =>
          println("Enter minimum threshold (or leave empty for no minimum):")
          val minStr = StdIn.readLine().trim
          val min = if (minStr.isEmpty) None else Try(minStr.toDouble).toOption
          
          println("Enter maximum threshold (or leave empty for no maximum):")
          val maxStr = StdIn.readLine().trim
          val max = if (maxStr.isEmpty) None else Try(maxStr.toDouble).toOption
          
          val thresholded = DataAnalyzer.applyThresholds(data, min, max)
          println(s"Applied thresholds: min=${min.getOrElse("none")}, max=${max.getOrElse("none")}")
          ConsoleUI.displayEnergyData(thresholded)

        case "4" => // Back to analysis menu
        
        case _ =>
          println("Invalid choice. Please try again.")
          print("Enter your choice: ")
          readValidChoice()
      }
    }
    
    readValidChoice()
  }
  
  // Start the application
  runApplication()
}