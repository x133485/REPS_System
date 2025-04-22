package services

import models._
import java.time._

/**
 * Service for managing and filtering energy data
 */
object DataManager {
  /**
   * Fetch data for all energy sources within a time range
   * 
   * @param startTime Start of time range (Unix timestamp)
   * @param endTime End of time range (Unix timestamp)
   * @param solarOn Whether to fetch solar data
   * @param windOn Whether to fetch wind data
   * @param hydroOn Whether to fetch hydro data
   * @return Combined list of data from all energy sources
   */
  def fetchAllData(
    startTime: Long, 
    endTime: Long, 
    solarOn: Boolean = true, 
    windOn: Boolean = true, 
    hydroOn: Boolean = true
  ): List[EnergyData] = {
    // Only include sources that are toggled on
    val sources = List(
      if (solarOn) Some(Solar) else None,
      if (windOn) Some(Wind) else None,
      if (hydroOn) Some(Hydro) else None
    ).flatten
    
    val allData = sources.flatMap { source =>
      val data = FingridClient.fetchEnergyData(source, startTime, endTime)
      Thread.sleep(2000)
      data
    }
    allData
  }
  
  /**
   * Fetch data for a specific energy source within a time range
   */
  def fetchDataForSource(source: EnergySource, startTime: Long, endTime: Long): List[EnergyData] = {
    FingridClient.fetchEnergyData(source, startTime, endTime)
  }
  
  /**
   * Get data for the current day
   */
  def fetchTodayData(): List[EnergyData] = {
    val now = LocalDateTime.now()
    val startOfDay = now.withHour(0).withMinute(0).withSecond(0)
    val startTime = startOfDay.atZone(ZoneId.systemDefault()).toEpochSecond
    val endTime = Instant.now().getEpochSecond
    
    fetchAllData(startTime, endTime)
  }
  
  /**
   * Get data for a specific day
   */
  def fetchDataForDay(
    year: Int, 
    month: Int, 
    day: Int,
    solarOn: Boolean = true, 
    windOn: Boolean = true, 
    hydroOn: Boolean = true
  ): List[EnergyData] = {
    val startDateTime = LocalDateTime.of(year, month, day, 0, 0)
    val endDateTime = LocalDateTime.of(year, month, day, 23, 59, 59)
    
    val startTime = startDateTime.atZone(ZoneId.systemDefault()).toEpochSecond
    val endTime = endDateTime.atZone(ZoneId.systemDefault()).toEpochSecond
    
    fetchAllData(startTime, endTime, solarOn, windOn, hydroOn)
  }
  
  /**
   * Get data for the last N hours
   */
  def fetchLastHoursData(
    hours: Int, 
    solarOn: Boolean = true, 
    windOn: Boolean = true, 
    hydroOn: Boolean = true
  ): List[EnergyData] = {
    val now = Instant.now().getEpochSecond
    val hoursAgo = now - (hours * 3600)
    
    fetchAllData(hoursAgo, now, solarOn, windOn, hydroOn)
  }
  
  /**
   * Get data for a specific week
   */
  def fetchDataForWeek(
    year: Int, 
    weekNumber: Int,
    solarOn: Boolean = true, 
    windOn: Boolean = true, 
    hydroOn: Boolean = true
  ): List[EnergyData] = {
    // Calculate the start date of the week
    val firstDayOfWeek = LocalDateTime
      .now()
      .withYear(year)
      .`with`(java.time.temporal.WeekFields.ISO.weekOfYear(), weekNumber)
      .`with`(java.time.temporal.WeekFields.ISO.dayOfWeek(), 1)
      .withHour(0).withMinute(0).withSecond(0)
      
    val lastDayOfWeek = firstDayOfWeek.plusDays(6).withHour(23).withMinute(59).withSecond(59)
    
    val startTime = firstDayOfWeek.atZone(ZoneId.systemDefault()).toEpochSecond
    val endTime = lastDayOfWeek.atZone(ZoneId.systemDefault()).toEpochSecond
    
    fetchAllData(startTime, endTime, solarOn, windOn, hydroOn)
  }
  
  /**
   * Get data for a specific month
   */
  def fetchDataForMonth(
    year: Int, 
    month: Int,
    solarOn: Boolean = true, 
    windOn: Boolean = true, 
    hydroOn: Boolean = true
  ): List[EnergyData] = {
    val startDateTime = LocalDateTime.of(year, month, 1, 0, 0)
    val endDateTime = startDateTime.plusMonths(1).minusSeconds(1)
    
    val startTime = startDateTime.atZone(ZoneId.systemDefault()).toEpochSecond
    val endTime = endDateTime.atZone(ZoneId.systemDefault()).toEpochSecond
    
    fetchAllData(startTime, endTime, solarOn, windOn, hydroOn)
  }
}