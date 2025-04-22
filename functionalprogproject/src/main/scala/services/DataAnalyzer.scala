package services

import models._
import java.time._
import java.time.temporal.WeekFields
import java.util.Locale

object DataAnalyzer {
  // Filter data by time periods
  def filterByHour(data: List[EnergyData], hour: Int): List[EnergyData] = {
    data.filter(d => {
      val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochSecond(d.timestamp), 
        ZoneId.systemDefault()
      )
      dateTime.getHour == hour
    })
  }
  
  def filterByDay(data: List[EnergyData], day: Int): List[EnergyData] = {
    data.filter(d => {
      val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochSecond(d.timestamp),
        ZoneId.systemDefault()
      )
      dateTime.getDayOfMonth == day
    })
  }
  
  def filterByWeek(data: List[EnergyData], weekOfYear: Int): List[EnergyData] = {
    data.filter(d => {
      val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochSecond(d.timestamp),
        ZoneId.systemDefault()
      )
      dateTime.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()) == weekOfYear
    })
  }
  
  def filterByMonth(data: List[EnergyData], month: Int): List[EnergyData] = {
    data.filter(d => {
      val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochSecond(d.timestamp),
        ZoneId.systemDefault()
      )
      dateTime.getMonthValue == month
    })
  }

  // Calculate mean using recursion instead of iteration
  def calculateMean(data: List[Double]): Option[Double] = {
    @scala.annotation.tailrec
    def sumRec(values: List[Double], acc: Double): Double = values match {
      case Nil => acc
      case head :: tail => sumRec(tail, acc + head)
    }
    
    if (data.isEmpty) None
    else Some(sumRec(data, 0.0) / data.length)
  }
  
  // Calculate median recursively
  def calculateMedian(data: List[Double]): Option[Double] = {
    def findMedian(sortedList: List[Double]): Double = {
      val n = sortedList.length
      if (n % 2 == 0) {
        val middleRight = n / 2
        val middleLeft = middleRight - 1
        (sortedList(middleLeft) + sortedList(middleRight)) / 2
      } else {
        sortedList(n / 2)
      }
    }
    
    if (data.isEmpty) None
    else Some(findMedian(data.sorted))
  }
  
  // Calculate mode using functional approach
  def calculateMode(data: List[Double]): Option[Double] = {
    if (data.isEmpty) None
    else {
      val grouped = data.groupBy(identity).map { case (value, occurrences) => (value, occurrences.size) }
      val maxOccurrences = grouped.values.max
      Some(grouped.filter(_._2 == maxOccurrences).keys.head)
    }
  }
  
  // Calculate range
  def calculateRange(data: List[Double]): Option[Double] = {
    if (data.isEmpty) None
    else Some(data.max - data.min)
  }
  
  // Calculate midrange
  def calculateMidrange(data: List[Double]): Option[Double] = {
    if (data.isEmpty) None
    else Some((data.min + data.max) / 2)
  }
  
  // Search functionality
  def searchByEnergyOutput(data: List[EnergyData], min: Double, max: Double): List[EnergyData] = {
    data.filter(d => d.energyOutput >= min && d.energyOutput <= max)
  }
  
  def searchByStatus(data: List[EnergyData], status: String): List[EnergyData] = {
    data.filter(_.status.toLowerCase == status.toLowerCase)
  }
}