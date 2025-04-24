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
    // For large date ranges, split into monthly chunks
    if (endTime - startTime > 30 * 24 * 3600) { // More than 30 days
      println(s"Large date range detected (${(endTime - startTime)/(24*3600)} days). Splitting into monthly chunks...")
      
      // Split into monthly chunks
      val oneMonthSeconds = 30 * 24 * 3600L
      
      // Create sequence of start times, one month apart
      val startTimes = Iterator.iterate(startTime)(t => t + oneMonthSeconds)
        .takeWhile(_ < endTime)
        .toList
        
      // For each start time, calculate end time (either one month later or the original end time)
      val timeChunks = startTimes.map(start => 
        (start, math.min(start + oneMonthSeconds, endTime))
      )
      
      println(s"Fetching data in ${timeChunks.size} chunks...")
      
      // Process each chunk and combine results
      timeChunks.flatMap { case (chunkStart, chunkEnd) => 
        println(s"Fetching chunk: ${formatTimestamp(chunkStart)} to ${formatTimestamp(chunkEnd)}")
        val result = fetchSingleChunk(source, chunkStart, chunkEnd)
        println(s"Got ${result.size} records for this chunk")
        result
      }
    } else {
      // For smaller ranges, use the original method
      fetchSingleChunk(source, startTime, endTime)
    }
  }

  // Update the fetchSingleChunk method with better error handling and retries
  private def fetchSingleChunk(source: EnergySource, startTime: Long, endTime: Long): List[EnergyData] = {
    val variableId = source match {
      case Solar => solarVariableId
      case Wind => windVariableId
      case Hydro => hydroVariableId
    }
    
    // Convert timestamps to Fingrid API format (ISO 8601)
    val startTimeStr = formatTimestamp(startTime)
    val endTimeStr = formatTimestamp(endTime)
    
    def fetchPage(page: Int, retriesLeft: Int = 3): List[FingridEvent] = {
      if (retriesLeft <= 0) {
        println(s"Failed to fetch data after multiple attempts for ${source}. Please check network connection or API availability.")
        return Nil
      }
      
      // Build API request
      val request = basicRequest
        .header("x-api-key", apiKey)
        .header("Accept", "application/json")
        .get(uri"$apiBaseUrl/datasets/$variableId/data?startTime=$startTimeStr&endTime=$endTimeStr&page=$page&pageSize=1000")
      
      try {
        println(s"Requesting page $page for ${source}...")
        val response = Await.result(request.send(backend), 30.seconds) // Increased timeout
        
        response.body match {
          case Right(jsonString) => 
            // Check for rate limiting
            if (jsonString.contains("rate limit") || response.code.code == 429) {
              println(s"Rate limit hit for Fingrid API. Waiting 10 seconds before retry...")
              Thread.sleep(10000)
              return fetchPage(page, retriesLeft - 1)
            }
            
            val cursor = parser.parse(jsonString).getOrElse(Json.Null).hcursor
            val events = cursor.downField("data").as[List[FingridEvent]].getOrElse(Nil)
            
            // Check if data structure is unexpected
            if (events.isEmpty && !jsonString.contains("\"data\":[]")) {
              println(s"Unexpected data format received. API may have changed. Response: ${jsonString.take(200)}...")
              Nil
            } else {
              val lastPage = cursor.downField("pagination").get[Int]("lastPage").getOrElse(1)
              val currentPage = cursor.downField("pagination").get[Int]("currentPage").getOrElse(page)
              println(s"Retrieved ${events.size} events (page $currentPage of $lastPage)")
              
              if (currentPage < lastPage) {
                events ++ fetchPage(currentPage + 1)
              } else {
                events
              }
            }
            
          case Left(error) =>
            println(s"Error with Fingrid API response (attempt ${4-retriesLeft}/3): $error")
            // Handle common error codes
            if (error.contains("404")) {
              println("Resource not found - please check that the variable ID is correct")
              Nil
            } else if (error.contains("403")) {
              println("Authentication failed - please check your API key")
              Nil
            } else {
              // For other errors, retry
              println(s"Retrying in 5 seconds...")
              Thread.sleep(5000)
              fetchPage(page, retriesLeft - 1)
            }
        }
      } catch {
        case e: java.net.SocketTimeoutException => 
          println(s"Network timeout (attempt ${4-retriesLeft}/3). Retrying in 5 seconds...")
          Thread.sleep(5000)
          fetchPage(page, retriesLeft - 1)
          
        case e: java.net.UnknownHostException =>
          println(s"Network error: Unable to reach the Fingrid API server. Please check your internet connection.")
          Nil
          
        case e: Exception => 
          println(s"Exception when fetching data from Fingrid API: ${e.getMessage}")
          if (retriesLeft > 1) {
            println(s"Retrying in 5 seconds...")
            Thread.sleep(5000)
            fetchPage(page, retriesLeft - 1)
          } else {
            Nil
          }
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
    // Lower thresholds based on more realistic Fingrid API data
    val thresholds = source match {
      case Solar => (100.0, 400.0)   // Lower solar threshold
      case Wind => (800.0, 2000.0)   // Lower wind threshold
      case Hydro => (1000.0, 1800.0) // Lower hydro threshold
    }
    
    if (output < thresholds._1) "Low"
    else if (output > thresholds._2) "High"
    else "Normal"
  }
}