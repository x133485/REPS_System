package services

import models._
import java.io._
import java.time.{Instant, LocalDateTime, ZoneId}
import scala.io.Source

object DataStorage {
  
  def loadAllDataFromFile(filename: String): Either[String, List[EnergyData]] = {
    val file = new java.io.File(filename)
    if (!file.exists()) {
      Left(s"File '$filename' does not exist.")
    } else {
      try {
        val source = scala.io.Source.fromFile(filename)
        try {
          // Parse our custom file format with sections
          val lines = source.getLines().toList
          val data = parseCustomFileFormat(lines)
          Right(data)
        } catch {
          case e: Exception => Left(s"Error reading file: ${e.getMessage}")
        } finally {
          source.close()
        }
      } catch {
        case e: Exception => Left(s"Error accessing file: ${e.getMessage}")
      }
    }
  }

  private def parseCustomFileFormat(lines: List[String]): List[EnergyData] = {
    var currentSection: Option[EnergySource] = None
    var dataStarted = false
    var result = List.empty[EnergyData]
    
    // Process each line
    for (line <- lines) {
      if (line.trim.isEmpty) {
        // Empty line - reset state for next section
        dataStarted = false
      } else if (line.startsWith("===== SOLAR =====")) {
        currentSection = Some(Solar)
        dataStarted = false
      } else if (line.startsWith("===== HYDRO =====")) {
        currentSection = Some(Hydro)
        dataStarted = false
      } else if (line.startsWith("===== WIND =====")) {
        currentSection = Some(Wind)
        dataStarted = false
      } else if (line.startsWith("timestamp,source,")) {
        // This is the column header line
        dataStarted = true
      } else if (line.startsWith("NO DATA FOR")) {
        // Informational message - skip
        dataStarted = false
      } else if (dataStarted && currentSection.isDefined) {
        // This appears to be an actual data line
        try {
          val parts = line.split(",")
          if (parts.length >= 5) {
            val timestamp = java.time.LocalDateTime
              .parse(parts(0), java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
              .atZone(java.time.ZoneId.systemDefault())
              .toEpochSecond
              
            val energyData = EnergyData(
              timestamp = timestamp,
              source = currentSection.get,
              energyOutput = parts(2).toDouble,
              location = parts(3),
              status = parts(4)
            )
            result = energyData :: result
          }
        } catch {
          case _: Exception => 
            // Skip invalid lines without failing the whole import
        }
      }
    }
    
    result.reverse // Maintain the original order
  }

  private def parseLine(line: String): EnergyData = {
    val parts = line.split(",")
    EnergyData(
      timestamp = java.time.LocalDateTime
        .parse(parts(0), java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        .atZone(java.time.ZoneId.systemDefault())
        .toEpochSecond,
      source = parseSource(parts(1)),
      energyOutput = parts(2).toDouble,
      location = parts(3),
      status = parts(4)
    )
  }
  
  private def parseSource(str: String): EnergySource = str match {
    case "Solar" => Solar
    case "Wind" => Wind
    case "Hydro" => Hydro
    case _ => throw new IllegalArgumentException(s"Unknown energy source: $str")
  }

  def saveDataListToFile(data: List[EnergyData], filename: String): Either[String, Unit] = {
    def formatDateTime(ts: Long): String = {
      val dateTime = java.time.LocalDateTime.ofInstant(
        java.time.Instant.ofEpochSecond(ts),
        java.time.ZoneId.systemDefault()
      )
      dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
    }

    if (!(filename.endsWith(".csv") || filename.endsWith(".xls"))) {
      Left("Invalid file extension. Please use .csv or .xls only.")
    } else {
      try {
        val writer = new PrintWriter(new FileWriter(filename, false))
        val sources = List(Solar, Hydro, Wind)
        sources.foreach { source =>
          val sourceData = data.filter(_.source == source)
          val label = source match {
            case Solar => "===== SOLAR ====="
            case Hydro => "===== HYDRO ====="
            case Wind  => "===== WIND ====="
          }
          val sourceName = source match {
            case Solar => "SOLAR"
            case Hydro => "HYDRO"
            case Wind  => "WIND" 
          }
          
          writer.println(label)
          writer.println("timestamp,source,energyOutput,location,status")
          
          if (sourceData.isEmpty) {
            // No data for this source - add informative message
            writer.println(s"\nNO DATA FOR $sourceName. IT WAS PROBABLY TURNED OFF WHILE THIS FILE WAS SAVED OR THERE IS ACTUALLY NO DATA FOR $sourceName")
          } else {
            // Write data if available
            sourceData.foreach { d =>
              writer.println(s"${formatDateTime(d.timestamp)},${d.source},${d.energyOutput},${d.location},${d.status}")
            }
          }
          
          writer.println() // Blank line between sections
        }
        writer.close()
        Right(())
      } catch {
        case e: Exception => Left(s"Error saving file: ${e.getMessage}")
      }
    }
  }
}