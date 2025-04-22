package services

import models._
import sttp.client3._
import sttp.client3.akkahttp._
import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._
import io.circe._
import io.circe.parser._
import io.circe.generic.semiauto._
import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

/**
 * Client for retrieving energy data from Fingrid API
 */
object FingridClient {
  // API configuration
  private val apiBaseUrl = "https://data.fingrid.fi/api"
  private val apiKey = "1837c5d0ba7d4c40b37a5ec4811e6dd4" // You need to register at Fingrid to get an API key
  
  // Variable IDs for different energy sources
  private val solarVariableId = 248  // Solar power forecast
  private val windVariableId = 181   // Wind power production
  private val hydroVariableId = 191  // Hydro power production
  
  // Add an implicit ActorSystem
  implicit val system = ActorSystem()
  // Create the Akka HTTP backend
  private val backend = AkkaHttpBackend.usingActorSystem(system)
  
  // Case class for parsing Fingrid API responses
  case class FingridEvent(
    datasetId: Int,
    startTime: String,
    endTime: String,
    value: Double
  )
  
  // Add implicit decoder for FingridEvent
  implicit val fingridEventDecoder: Decoder[FingridEvent] = deriveDecoder[FingridEvent]
  
  case class FingridResponse(
    data: List[FingridEvent]
  )
  implicit val fingridResponseDecoder: Decoder[FingridResponse] = deriveDecoder[FingridResponse]
  
  /**
   * Fetch energy data from Fingrid API for a specific energy source within a time range
   */
  def fetchEnergyData(source: EnergySource, startTime: Long, endTime: Long): List[EnergyData] = {
    val variableId = source match {
      case Solar => solarVariableId
      case Wind => windVariableId
      case Hydro => hydroVariableId
    }
    
    // Convert timestamps to Fingrid API format (ISO 8601)
    val startTimeStr = formatTimestamp(startTime)
    val endTimeStr = formatTimestamp(endTime)
    
    def fetchPage(page: Int): List[FingridEvent] = {
      // Build API request
      val request = basicRequest
        .header("x-api-key", apiKey)
        .header("Accept", "application/json")
        .get(uri"$apiBaseUrl/datasets/$variableId/data?startTime=$startTimeStr&endTime=$endTimeStr&page=$page&pageSize=10000")
      
      try {
        val response = Await.result(request.send(backend), 10.seconds)
        
        response.body match {
          case Right(jsonString) => 
            val cursor = parser.parse(jsonString).getOrElse(Json.Null).hcursor
            val events = cursor.downField("data").as[List[FingridEvent]].getOrElse(Nil)
            val lastPage = cursor.downField("pagination").get[Int]("lastPage").getOrElse(1)
            val currentPage = cursor.downField("pagination").get[Int]("currentPage").getOrElse(page)
            if (currentPage < lastPage) {
              events ++ fetchPage(currentPage + 1)
            } else {
              events
            }
          case Left(_) => Nil
        }
      } catch {
        case _: Exception => Nil
      }
    }
    
    fetchPage(1).map(event => convertToEnergyData(event, source))
  }
  
  /**
   * Format timestamp for Fingrid API (ISO 8601)
   */
  private def formatTimestamp(timestamp: Long): String = {
    val dateTime = LocalDateTime.ofInstant(
      Instant.ofEpochSecond(timestamp),
      ZoneId.of("UTC")
    )
    dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z"
  }
  
  /**
   * Parse JSON response from Fingrid API
   */
  private def parseResponse(jsonString: String, source: EnergySource): List[EnergyData] = {
    decode[FingridResponse](jsonString) match {
      case Right(response) =>
        response.data.map(event => convertToEnergyData(event, source))
      case Left(error) =>
        println(s"Error parsing Fingrid API response: $error")
        List.empty
    }
  }
  
  /**
   * Convert Fingrid event to our EnergyData model
   */
  private def convertToEnergyData(event: FingridEvent, source: EnergySource): EnergyData = {
    // Parse ISO timestamp to Unix timestamp - using startTime from API
    val timestamp = Instant.parse(event.startTime).getEpochSecond
      
    val location = source match {
      case Solar => "Fingrid Solar Plant"
      case Wind => "Fingrid Wind Farm"
      case Hydro => "Fingrid Hydro Station"
    }
    
    val status = determineStatus(event.value, source)
    
    EnergyData(
      timestamp = timestamp,
      source = source,
      energyOutput = event.value,
      location = location,
      status = status
    )
  }
  
  /**
   * Determine status based on energy output value
   */
  private def determineStatus(output: Double, source: EnergySource): String = {
    // Thresholds based on observed Fingrid API data
    val thresholds = source match {
      case Solar => (300.0, 500.0)   // Observed around 400+ MW
      case Wind => (1800.0, 2200.0)  // Observed around 2000+ MW
      case Hydro => (1700.0, 1900.0) // Observed around 1800+ MW
    }
    
    if (output < thresholds._1) "Low"
    else if (output > thresholds._2) "High"
    else "Normal"
  }
}