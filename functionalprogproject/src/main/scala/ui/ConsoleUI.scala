package ui

import models._
import services._
import java.time.{Instant, LocalDateTime, ZoneId, LocalDate}
import java.time.format.DateTimeFormatter
import scala.io.StdIn
import scala.util.{Try, Success, Failure}

object ConsoleUI {
  def displayMenu(): Unit = {
    println("\n===== RENEWABLE ENERGY PLANT SYSTEM =====")
    println("1. Collect Real-Time Energy Data")
    println("2. View Energy Data")
    println("3. Analyze Energy Data")
    println("4. Check Alerts")
    println("5. View Data from File")
    println("6. Control Plant Operation")
    println("7. Save Data to File")
    println("8. Exit")
    print("Enter your choice: ")
  }
  
  def formatDateTime(timestamp: Long): String = {
    val dateTime = LocalDateTime.ofInstant(
      Instant.ofEpochSecond(timestamp),
      ZoneId.systemDefault()
    )
    dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
  }
  
  def displayEnergyData(data: List[EnergyData]): Unit = {
    // Process visible sources (may have some toggled off)
    val sourcesList = List(Solar, Hydro, Wind)
    val visibleSources = sourcesList.filter(source => data.exists(_.source == source))
    
    if (visibleSources.isEmpty) {
      println("\nNo energy data available to display.")
      return
    }
    
    val pageSize = 100
    var currentSourceIndex = 0
    var currentPage = 0
    var exitPagination = false
    
    while (!exitPagination && currentSourceIndex < visibleSources.length) {
      val currentSource = visibleSources(currentSourceIndex)
      val sourceData = data.filter(_.source == currentSource)
      val totalPagesForSource = (sourceData.length + pageSize - 1) / pageSize
      
      // Determine label for current source
      val label = currentSource match {
        case Solar => "SOLAR"
        case Hydro => "HYDRO"
        case Wind  => "WIND"
      }
      
      // Display current page for current source
      println(s"\n===== $label =====")
      println("Timestamp            | Source | Output | Location      | Status")
      println("--------------------|--------|--------|--------------|--------")
      sourceData.slice(currentPage * pageSize, (currentPage + 1) * pageSize).foreach { d =>
        println(f"${formatDateTime(d.timestamp)} | ${d.source}%-6s | ${d.energyOutput}%6.2f | ${d.location}%-12s | ${d.status}")
      }
      
      // Show pagination status and prompt
      val fromRecord = currentPage * pageSize + 1
      val toRecord = Math.min((currentPage + 1) * pageSize, sourceData.length)
      println(s"\nShowing records $fromRecord to $toRecord of ${sourceData.length} for $label")
      
      var validInput = false
      while (!validInput) {
        // Customize prompt based on pagination position
        if (currentPage == 0 && currentSourceIndex == 0) {
          // First page of first source - can only go forward or quit
          println("Press Enter for next page, or 'q' + Enter to quit viewing data.")
        } else if (currentPage >= totalPagesForSource - 1 && currentSourceIndex >= visibleSources.length - 1) {
          // Last page of last source - can only go backward or quit
          println("Press 'p' + Enter for previous page, or 'q' + Enter to quit viewing data.")
        } else {
          // Middle pages - can go in either direction or quit
          println("Press Enter for next page, 'p' + Enter for previous page, or 'q' + Enter to quit viewing data.")
        }
        
        val input = scala.io.StdIn.readLine().trim.toLowerCase
        
        input match {
          case "q" => 
            // Exit the entire pagination
            exitPagination = true
            validInput = true
            
          case "p" => 
            // Go to previous page, potentially previous source
            if (currentPage > 0) {
              // Go to previous page within current source
              currentPage -= 1
              validInput = true
            } else if (currentSourceIndex > 0) {
              // Go to previous source, last page
              currentSourceIndex -= 1
              val prevSourceData = data.filter(_.source == visibleSources(currentSourceIndex))
              currentPage = (prevSourceData.length - 1) / pageSize
              validInput = true
            } else {
              println("You are at the first page of the first source.")
            }
            
          case "" => 
            // Go to next page, potentially next source
            if (currentPage < totalPagesForSource - 1) {
              // Go to next page within current source
              currentPage += 1
              validInput = true
            } else if (currentSourceIndex < visibleSources.length - 1) {
              // Go to next source, first page
              currentSourceIndex += 1
              currentPage = 0
              validInput = true
            } else {
              println("You are at the last page of the last source.")
            }
            
          case _ =>
            println("Invalid input. Please use Enter, 'p' + Enter, or 'q' + Enter.")
        }
      }
    }
  }
  
  def displayAnalysisMenu(): Unit = {
    println("\n===== DATA ANALYSIS =====")
    println("1. Calculate Statistics")
    println("2. Filter by Date Range")
    println("3. Filter by Hour")
    println("4. Filter by Day")
    println("5. Filter by Week")
    println("6. Filter by Month")
    println("7. Search by Status")
    println("8. Back to Main Menu")
    print("Enter your choice: ")
  }
  
  def displayStatistics(data: List[EnergyData]): Unit = {
    val sources = List(Solar, Wind, Hydro)
    println("\n===== STATISTICAL ANALYSIS BY SOURCE =====")
    sources.foreach { source =>
      val sourceData = data.filter(_.source == source)
      val outputs = sourceData.map(_.energyOutput)
      val label = source match {
        case Solar => "Solar"
        case Wind  => "Wind"
        case Hydro => "Hydro"
      }
      println(s"\n--- $label ---")
      DataAnalyzer.calculateMean(outputs) match {
        case Some(value) => println(f"Mean: $value%.2f")
        case None => println("Mean: N/A")
      }
      DataAnalyzer.calculateMedian(outputs) match {
        case Some(value) => println(f"Median: $value%.2f")
        case None => println("Median: N/A")
      }
      DataAnalyzer.calculateMode(outputs) match {
        case Some(value) => println(f"Mode: $value%.2f")
        case None => println("Mode: N/A")
      }
      DataAnalyzer.calculateRange(outputs) match {
        case Some(value) => println(f"Range: $value%.2f")
        case None => println("Range: N/A")
      }
      DataAnalyzer.calculateMidrange(outputs) match {
        case Some(value) => println(f"Midrange: $value%.2f")
        case None => println("Midrange: N/A")
      }
    }
  }
  
  def displayAlerts(alerts: List[AlertSystem.Alert]): Unit = {
    println("\n===== SYSTEM ALERTS =====")
    if (alerts.isEmpty) {
      println("No alerts detected.")
      return
    }
    
    // Process visible sources
    val sourcesList = List(Solar, Hydro, Wind)
    val visibleSources = sourcesList.filter(source => alerts.exists(_.source == source))
    
    if (visibleSources.isEmpty) {
      println("\nNo alert data available to display.")
      return
    }
    
    val pageSize = 100
    var currentSourceIndex = 0
    var currentPage = 0
    var exitPagination = false
    
    while (!exitPagination && currentSourceIndex < visibleSources.length) {
      val currentSource = visibleSources(currentSourceIndex)
      val sourceAlerts = alerts.filter(_.source == currentSource)
      val totalPagesForSource = (sourceAlerts.length + pageSize - 1) / pageSize
      
      // Determine label for current source
      val label = currentSource match {
        case Solar => "SOLAR"
        case Hydro => "HYDRO"
        case Wind  => "WIND"
      }
      
      // Display current page for current source
      println(s"\n===== $label =====")
      println("Time               | Type                     | Source | Message")
      println("------------------|--------------------------|--------|--------")
      sourceAlerts.slice(currentPage * pageSize, (currentPage + 1) * pageSize).foreach { a =>
        val alertType = a.alertType match {
          case AlertSystem.LowOutputAlert => "Low Output"
          case AlertSystem.HighOutputAlert => "High Output"
          case AlertSystem.EquipmentMalfunctionAlert => "Equipment Malfunction"
        }
        println(f"${formatDateTime(a.timestamp)} | ${alertType}%-24s | ${a.source}%-6s | ${a.message}")
      }
      
      // Show pagination status and prompt
      val fromRecord = currentPage * pageSize + 1
      val toRecord = Math.min((currentPage + 1) * pageSize, sourceAlerts.length)
      println(s"\nShowing alerts $fromRecord to $toRecord of ${sourceAlerts.length} for $label")
      
      var validInput = false
      while (!validInput) {
        // Customize prompt based on pagination position
        if (currentPage == 0 && currentSourceIndex == 0) {
          // First page of first source - can only go forward or quit
          println("Press Enter for next page, or 'q' + Enter to quit viewing alerts.")
        } else if (currentPage >= totalPagesForSource - 1 && currentSourceIndex >= visibleSources.length - 1) {
          // Last page of last source - can only go backward or quit
          println("Press 'p' + Enter for previous page, or 'q' + Enter to quit viewing alerts.")
        } else {
          // Middle pages - can go in either direction or quit
          println("Press Enter for next page, 'p' + Enter for previous page, or 'q' + Enter to quit viewing alerts.")
        }
        
        val input = scala.io.StdIn.readLine().trim.toLowerCase
        
        input match {
          case "q" => 
            // Exit the entire pagination
            exitPagination = true
            validInput = true
            
          case "p" => 
            // Go to previous page, potentially previous source
            if (currentPage > 0) {
              // Go to previous page within current source
              currentPage -= 1
              validInput = true
            } else if (currentSourceIndex > 0) {
              // Go to previous source, last page
              currentSourceIndex -= 1
              val prevSourceAlerts = alerts.filter(_.source == visibleSources(currentSourceIndex))
              currentPage = (prevSourceAlerts.length - 1) / pageSize
              validInput = true
            } else {
              println("You are at the first page of the first source.")
            }
            
          case "" => 
            // Go to next page, potentially next source
            if (currentPage < totalPagesForSource - 1) {
              // Go to next page within current source
              currentPage += 1
              validInput = true
            } else if (currentSourceIndex < visibleSources.length - 1) {
              // Go to next source, first page
              currentSourceIndex += 1
              currentPage = 0
              validInput = true
            } else {
              println("You are at the last page of the last source.")
            }
            
          case _ =>
            println("Invalid input. Please use Enter, 'p' + Enter, or 'q' + Enter.")
        }
      }
    }
  }
  
  def promptForDateRange(): Option[(Long, Long)] = {
    def readDate(prompt: String): LocalDate = {
      val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
      while (true) {
        println(prompt)
        val input = StdIn.readLine()
        try {
          return LocalDate.parse(input, formatter)
        } catch {
          case _: Exception =>
            println("Invalid date format. Please enter the date in the format 'DD/MM/YYYY'.")
            println("For example, enter '12/04/2024' for April 12, 2024.")
        }
      }
      LocalDate.now() // unreachable
    }
    val startDate = readDate("\nEnter start date (DD/MM/YYYY):")
    val endDate = readDate("Enter end date (DD/MM/YYYY):")
    if (endDate.isBefore(startDate)) {
      println("Error: End date cannot be before start date.")
      None
    } else {
      val startTime = startDate.atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond
      val endTime = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond
      Some((startTime, endTime))
    }
  }
  
  def promptForHour(): Option[Int] = {
    while (true) {
      println("\nEnter hour (0-23):")
      try {
        val hour = StdIn.readLine().toInt
        if (hour >= 0 && hour <= 23) return Some(hour)
        else throw new Exception()
      } catch {
        case _: Exception =>
          println("Error: Invalid hour. Please enter a value between 0 and 23.")
      }
    }
    None // unreachable
  }
  
  def promptForDay(): Option[(Int, Int, Int)] = {
    while (true) {
      println("\nEnter year (YYYY):")
      val yearStr = StdIn.readLine()
      println("Enter month (1-12):")
      val monthStr = StdIn.readLine()
      println("Enter day (1-31):")
      val dayStr = StdIn.readLine()
      try {
        val year = yearStr.toInt
        val month = monthStr.toInt
        val day = dayStr.toInt
        LocalDate.of(year, month, day) // Will throw if invalid
        return Some((year, month, day))
      } catch {
        case _: Exception =>
          println("Error: Invalid date. Please enter a valid year (YYYY), month (1-12), and day (1-31).")
      }
    }
    None // unreachable
  }
  
  def promptForWeek(): Option[(Int, Int)] = {
    while (true) {
      println("\nEnter year (YYYY):")
      val yearStr = StdIn.readLine()
      println("Enter week number (1-53):")
      val weekStr = StdIn.readLine()
      try {
        val year = yearStr.toInt
        val week = weekStr.toInt
        if (week < 1 || week > 53) throw new Exception()
        return Some((year, week))
      } catch {
        case _: Exception =>
          println("Error: Invalid input. Please enter a valid year (YYYY) and week number (1-53).")
      }
    }
    None // unreachable
  }
  
  def promptForMonth(): Option[(Int, Int)] = {
    while (true) {
      println("\nEnter year (YYYY):")
      val yearStr = StdIn.readLine()
      println("Enter month (1-12):")
      val monthStr = StdIn.readLine()
      try {
        val year = yearStr.toInt
        val month = monthStr.toInt
        if (month < 1 || month > 12) throw new Exception()
        return Some((year, month))
      } catch {
        case _: Exception =>
          println("Error: Invalid input. Please enter a valid year (YYYY) and month (1-12).")
      }
    }
    None // unreachable
  }

  def displayStorageCapacity(data: List[EnergyData]): Unit = {
    // Simulate storage as sum of all outputs (or any logic you want)
    val total = data.map(_.energyOutput).sum
    val capacity = 100000.0 // Example max capacity
    val percent = if (capacity == 0) 0 else (total / capacity * 100).min(100)
    println(f"Simulated Storage Capacity: $percent%.2f%% (${total} / $capacity units)")
  }
}