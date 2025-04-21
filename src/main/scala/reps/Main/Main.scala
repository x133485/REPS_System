package reps.Main

import reps.io.{ApiDataFetcher, CSVDataStorage}
import reps.core.DataModel.{PowerForecast, WindSolarMetrics}

import java.time.{Instant, ZoneId}
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import scala.io.StdIn.readLine
import scala.util.Try

object Main {

  private val apiKey = "8dfae6133423485ea120af36075f78fb"
  private val urlSolar = "https://data.fingrid.fi/api/datasets/248/data"
  private val urlWind = "https://data.fingrid.fi/api/datasets/181/data"
  private val urlHydro191 = "https://data.fingrid.fi/api/datasets/191/data" // NEW
  private val zone = ZoneId.systemDefault()
  private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone)

  private var lastData: List[WindSolarMetrics] = Nil
  private var lastType:  String                = ""   // "solar" | "wind" | "hydro"

  def main(args: Array[String]): Unit =
    var running = true
    while running do
      println()
      println("===== Renewable Energy Console =====")
      println("1  - Solar power forecast (dataset 248)")
      println("2  - Wind generation        (dataset 181)")
      println("3  - Hydro generation       (dataset 191)")
      println("4  - Store last data to CSV")
      println("0  - Exit")
      print("Your choice: ")

      val choice = Try(readLine().trim.toInt).getOrElse(-1)

      choice match
        case 0 =>
          running = false
          println("Bye!")

        case 1 => runSolar()
        case 2 => runWindOrHydro(urlWind, "Wind")
        case 3 => runWindOrHydro(urlHydro191, "Hydro")
        case 4 => storeToCsv()
        case _ => println("Invalid choice.")

  /* ---------- Business functions ---------- */

  /** 15-minute solar forecast */
  private def runSolar(): Unit =
    val now = Instant.now()
    val res = ApiDataFetcher.fetchData[PowerForecast](
      urlSolar, apiKey, now, now.plus(45, ChronoUnit.MINUTES),
      r => PowerForecast(Instant.parse(r.startTime), Instant.parse(r.endTime), r.value)
    )

    res match
      case Right(list) =>
        val forecasts = list.sortBy(_.startTime).takeRight(3)
        println(s"Latest Solar forecasts (${forecasts.size} × 15 min):")
        forecasts.foreach { d =>
          println(f"${fmt.format(d.startTime)} ~ ${fmt.format(d.endTime)} : ${d.powerOutput}%.2f MW")
        }
        lastData = forecasts.map(d => WindSolarMetrics(d.startTime, d.endTime, d.powerOutput))
        lastType = "solar"
      case Left(err) => println(s"Error: $err")

  /** Reusable wind/new hydropower data printing */
  private def runWindOrHydro(url: String, label: String): Unit =
    val end = Instant.now()
    val start = end.minus(9, ChronoUnit.MINUTES) // 3 × 3‑minute points
    val res = ApiDataFetcher.fetchData[WindSolarMetrics](
      url, apiKey, start, end,
      r => WindSolarMetrics(Instant.parse(r.startTime), Instant.parse(r.endTime), r.value)
    )

    res match
      case Right(list) =>
        val dataPoints = list.sortBy(_.endTime).takeRight(3)
        println(s"Latest $label output (${dataPoints.size} × 3 min):")
        dataPoints.foreach{d =>
          println(f"${fmt.format(d.startTime)} ~ ${fmt.format(d.endTime)} : ${d.powerOutput}%.2f kW")}
        lastData = dataPoints
        lastType = label.toLowerCase
      case Left(err) => println(s"Error: $err")

  /* ---------- Input ---------- */
  private def choice(): Int =
    print("Enter choice: ")
    Try(readLine().trim.toInt).getOrElse(0)

  private def storeToCsv():Unit =
    if lastData.isEmpty then
      println("No cache data. Run option 1/2/3 first.")
    else
      println("Need to edit data before saving? (y/N): ")
      val edited = if readLine().trim.toLowerCase == "y" then
        lastData.map { d =>
          print(s"${fmt.format(d.startTime)} value ${d.powerOutput} -> ")
          val in = readLine().trim
          val newVal = Try(in.toDouble).getOrElse(d.powerOutput)
          d.copy(powerOutput = newVal)
        }
      else lastData

      val fileName = s"${lastType}.csv"
      CSVDataStorage.saveToCSV(edited,fileName)
      println(s"Saved to $fileName")
}