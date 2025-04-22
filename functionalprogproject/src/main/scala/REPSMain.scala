import models._
import services._
import ui._
import scala.io.StdIn
import scala.util.{Try, Success, Failure}
import java.time.{Instant, LocalDateTime, ZoneId}

object REPSMain extends App {  
  // Add a mutable variable to simulate storage level and source status
  var storageLevel: Double = 50000.0 // Example initial value
  var solarOn = true
  var windOn = true
  var hydroOn = true

  // Main application loop
  def runApplication(): Unit = {
    var running = true
    var data: List[EnergyData] = Nil
    
    while (running) {
      ConsoleUI.displayMenu()
      
      var validChoice = false
      while (!validChoice) {
        val choice = StdIn.readLine()
        
        choice match {
          case "1" => 
            // Fetch real-time energy data
            var validTimeChoice = false
            while (!validTimeChoice) {
              println("\n===== COLLECT REAL-TIME ENERGY DATA =====")
              println("1. Last hour")
              println("2. Last 24 hours")
              println("3. Last 7 days")
              println("4. Custom date range")
              println("5. Back to Main Menu")
              print("Enter your choice: ")
              
              val timeChoice = StdIn.readLine()
              
              timeChoice match {
                case "1" =>
                  println("Fetching data for the last hour...")
                  data = DataManager.fetchLastHoursData(1, solarOn, windOn, hydroOn)
                  ConsoleUI.displayEnergyData(data)
                  validTimeChoice = true
                case "2" =>
                  println("Fetching data for the last 24 hours...")
                  data = DataManager.fetchLastHoursData(24, solarOn, windOn, hydroOn)
                  ConsoleUI.displayEnergyData(data)
                  validTimeChoice = true
                case "3" =>
                  println("Fetching data for the last 7 days...")
                  val now = Instant.now().getEpochSecond
                  val sevenDaysAgo = now - (7 * 24 * 3600)
                  data = DataManager.fetchAllData(sevenDaysAgo, now, solarOn, windOn, hydroOn)
                  ConsoleUI.displayEnergyData(data)
                  validTimeChoice = true
                case "4" =>
                  ConsoleUI.promptForDateRange() match {
                    case Some((startTime, endTime)) =>
                      println("Fetching data for the specified date range...")
                      data = DataManager.fetchAllData(startTime, endTime, solarOn, windOn, hydroOn)
                      ConsoleUI.displayEnergyData(data)
                      validTimeChoice = true
                    case None =>
                      println("Operation cancelled.")
                      validTimeChoice = true
                  }
                case "5" =>
                  validTimeChoice = true // Just exit the submenu
                case _ =>
                  println("Invalid choice. Please try again.")
              }
            }
            validChoice = true
            
          case "2" =>
            // View energy data
            if (data.isEmpty) {
              println("\n===== ENERGY DATA =====")
              println("No data available to view. Please collect or load data first.")
            } else {
              ConsoleUI.displayEnergyData(data)
            }
            validChoice = true
            
          case "3" =>
            // Analyze energy data
            if (data.isEmpty) {
              println("\n===== DATA ANALYSIS =====")
              println("No data available to analyze. Please collect or load data first.")
            } else {
              var analyzing = true
              while (analyzing) {
                ConsoleUI.displayAnalysisMenu()
                val analysisChoice = StdIn.readLine()
                analysisChoice match {
                  case "1" =>
                    ConsoleUI.displayStatistics(data)
                    
                  case "2" =>
                    ConsoleUI.promptForDateRange() match {
                      case Some((startTime, endTime)) =>
                        val filteredData = data.filter(d => 
                          d.timestamp >= startTime && d.timestamp <= endTime)
                        ConsoleUI.displayEnergyData(filteredData)
                      case None => // Do nothing
                    }
                    
                  case "3" =>
                    ConsoleUI.promptForHour() match {
                      case Some(hour) =>
                        val filteredData = DataAnalyzer.filterByHour(data, hour)
                        ConsoleUI.displayEnergyData(filteredData)
                      case None => // Do nothing
                    }
                    
                  case "4" =>
                    ConsoleUI.promptForDay() match {
                      case Some((year, month, day)) =>
                        println(s"Fetching data for $year-$month-$day...")
                        val dayData = DataManager.fetchDataForDay(year, month, day, solarOn, windOn, hydroOn)
                        ConsoleUI.displayEnergyData(dayData)
                      case None => // Do nothing
                    }
                    
                  case "5" =>
                    ConsoleUI.promptForWeek() match {
                      case Some((year, week)) =>
                        println(s"Fetching data for week $week of $year...")
                        val weekData = DataManager.fetchDataForWeek(year, week, solarOn, windOn, hydroOn)
                        ConsoleUI.displayEnergyData(weekData)
                      case None => // Do nothing
                    }
                    
                  case "6" =>
                    ConsoleUI.promptForMonth() match {
                      case Some((year, month)) =>
                        println(s"Fetching data for $year-$month...")
                        val monthData = DataManager.fetchDataForMonth(year, month, solarOn, windOn, hydroOn)
                        ConsoleUI.displayEnergyData(monthData)
                      case None => // Do nothing
                    }
                    
                  case "7" =>
                    println("Enter status to search (e.g., Low, Normal, High):")
                    val status = StdIn.readLine()
                    val filteredData = DataAnalyzer.searchByStatus(data, status)
                    ConsoleUI.displayEnergyData(filteredData)
                    
                  case "8" =>
                    analyzing = false
                    
                  case "9" =>
                    println("Sort by: 1. Timestamp  2. Output  3. Source")
                    StdIn.readLine() match {
                      case "1" => ConsoleUI.displayEnergyData(data.sortBy(_.timestamp))
                      case "2" => ConsoleUI.displayEnergyData(data.sortBy(_.energyOutput))
                      case "3" => ConsoleUI.displayEnergyData(data.sortBy(_.source.toString))
                      case _ => println("Invalid sort option.")
                    }
                    
                  case _ =>
                    println("Invalid choice. Please try again.")
                }
              }
            }
            validChoice = true
            
          case "4" =>
            if (data.isEmpty) {
              println("\n===== SYSTEM ALERTS =====")
              println("No data available to check alerts. Please collect or load data first.")
            } else {
              val alerts = AlertSystem.detectIssues(data)
              ConsoleUI.displayAlerts(alerts)
            }
            validChoice = true
            
          case "5" =>
            println("Enter filename to load:")
            val filename = StdIn.readLine().trim
            DataStorage.loadAllDataFromFile(filename) match {
              case Left(errorMsg) =>
                println(errorMsg)
              case Right(fileData) =>
                if (fileData.isEmpty) {
                  println("No data available in the selected file.")
                } else {
                  data = fileData
                  ConsoleUI.displayEnergyData(data)
                }
            }
            validChoice = true

          case "6" =>
            var controlling = true
            while (controlling) {
              println("\n===== CONTROL PLANT OPERATION =====")
              println(s"Storage Level: $storageLevel units")
              println(s"1. Adjust Storage Level")
              println(s"2. Toggle Solar Panels (Currently: ${if (solarOn) "ON" else "OFF"})")
              println(s"3. Toggle Hydro Plant (Currently: ${if (hydroOn) "ON" else "OFF"})")
              println(s"4. Toggle Wind Turbines (Currently: ${if (windOn) "ON" else "OFF"})")
              println("5. Back to Main Menu")
              print("Enter your choice: ")
              StdIn.readLine() match {
                case "1" =>
                  println("Enter new storage level (0-100000):")
                  Try(StdIn.readLine().toDouble) match {
                    case Success(level) if level >= 0 && level <= 100000 =>
                      storageLevel = level
                      println(s"Storage level set to $storageLevel")
                    case _ =>
                      println("Invalid value. Please enter a number between 0 and 100000.")
                  }
                case "2" =>
                  solarOn = !solarOn
                  println(s"Solar panels turned " + (if (solarOn) "ON" else "OFF"))
                case "3" =>
                  hydroOn = !hydroOn
                  println(s"Hydro plant turned " + (if (hydroOn) "ON" else "OFF"))
                case "4" =>
                  windOn = !windOn
                  println(s"Wind turbines turned " + (if (windOn) "ON" else "OFF"))
                case "5" =>
                  controlling = false
                case _ =>
                  println("Invalid choice.")
              }
            }
            validChoice = true

          case "7" =>
            if (data.isEmpty) {
              println("No data to save. Please collect or view data first.")
            } else {
              var valid = false
              while (!valid) {
                println("Enter filename to save (must end with .csv or .xls):")
                val filename = StdIn.readLine().trim
                DataStorage.saveDataListToFile(data, filename) match {
                  case Right(_) =>
                    println(s"Data saved to $filename")
                    valid = true
                  case Left(errorMsg) =>
                    println(errorMsg)
                }
              }
            }
            validChoice = true
            
          case "8" =>
            println("Exiting Renewable Energy Plant System. Goodbye!")
            running = false
            validChoice = true
            
          case _ =>
            println("Invalid choice. Please try again.")
            print("Enter your choice: ")
        }
      }
    }
  }
  
  println("Starting Renewable Energy Plant System...")
  runApplication()
}