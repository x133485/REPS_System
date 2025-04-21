package reps.Main

import reps.io.ApiDataFetcher
import reps.core.DataModel

import java.time.{Instant, LocalDate, ZoneId}
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import scala.io.StdIn.readLine
import scala.util.Try

object Main {

  def main(args: Array[String]): Unit = {
    val apiKey = "8dfae6133423485ea120af36075f78fb"

    println("Here is the menu:")
    println("1 - Solar Forecast")
    println("2 - Wind Energy Data")
    println("3 - Hydropower Data (AV01)")

    val choice = getValidChoice()

    val now = Instant.now()

    val windApiUrl = "https://data.fingrid.fi/api/datasets/181/data"
    val solarForecastApiUrl = "https://data.fingrid.fi/api/datasets/248/data"
    val hydropowerApiUrl = "https://data.fingrid.fi/api/datasets/362/data"

    choice match {
      case 1 =>
        val solar_startTime = now
        val solar_endTime = now.plus(45, ChronoUnit.MINUTES)

        val solarForecastResult = ApiDataFetcher.fetchData[DataModel.PowerForecast](
          url = solarForecastApiUrl,
          apiKey = apiKey,
          startTime = solar_startTime,
          endTime = solar_endTime,
          parseFunc = raw => DataModel.PowerForecast(
            startTime = Instant.parse(raw.startTime),
            endTime = Instant.parse(raw.endTime),
            powerOutput = raw.value
          )
        )

        solarForecastResult match {
          case Right(data) =>
            val predictions = data.sortBy(_.startTime).take(3)
            println("=== The latest 2 solar power forecasts (15 minutes interval) ===")
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC+3"))
            predictions.foreach { d =>
              println(s"Time interval: ${formatter.format(d.startTime)} ~ ${formatter.format(d.endTime)}, Predicted power generation: ${d.powerOutput} megawatts")
            }
          case Left(error) =>
            println(s"Error: $error")
        }

      case 2 =>
        val endTime = now
        val startTime = endTime.minus(9, ChronoUnit.MINUTES)

        val windResult = ApiDataFetcher.fetchData[DataModel.WindMetrics](
          url = windApiUrl,
          apiKey = apiKey,
          startTime = startTime,
          endTime = endTime,
          parseFunc = raw => DataModel.WindMetrics(
            startTime = Instant.parse(raw.startTime),
            endTime = Instant.parse(raw.endTime),
            powerOutput = raw.value
          )
        )

        windResult match {
          case Right(data) =>
            val latestData = data.sortBy(_.endTime).takeRight(3)
            println("=== The latest 2 wind power generation data (3 minutes interval) ===")
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
            latestData.foreach { d =>
              println(s"Time range: ${formatter.format(d.startTime)} ~ ${formatter.format(d.endTime)}, Power Generation: ${d.powerOutput} kW")
            }
          case Left(error) =>
            println(s"Error: $error")
        }

      case 3 =>
        //The data renew every month.
        val zoneId = ZoneId.systemDefault()
        val now = LocalDate.now(zoneId)
        val firstDayOfLastMonth = now.minusMonths(1).withDayOfMonth(1)
        val lastDayOfLastMonth = firstDayOfLastMonth.plusMonths(1).minusDays(1)

        // Convert to Instant
        val startTime = firstDayOfLastMonth.atStartOfDay(zoneId).toInstant
        val endTime = lastDayOfLastMonth.atTime(23, 59, 59).atZone(zoneId).toInstant

        val HydropowerResult = ApiDataFetcher.fetchData[DataModel.HydropowerData](
          url = hydropowerApiUrl,
          apiKey = apiKey,
          startTime = startTime,
          endTime = endTime,
          parseFunc = raw => DataModel.HydropowerData(
            startTime = Instant.parse(raw.startTime),
            endTime = Instant.parse(raw.endTime),
            productionType = raw.additionalJson
              .flatMap(_.get("ProductionType"))
              .getOrElse("Unknown"),
            value = raw.value
          )
        )

        HydropowerResult match {
          case Right(data) =>
            val av01Data = data
              .filter(_.productionType == "AV01")
              .sortBy(_.startTime)

            println(s"=== AV01 Hydropower Production (${firstDayOfLastMonth.getMonthValue}-${firstDayOfLastMonth.getYear}) ===")

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
              .withZone(zoneId)

            if (av01Data.isEmpty) {
              println("No AV01 data found for last month")
            } else {
              // 计算总量并转换单位
              val totalKWH = av01Data.map(_.value).sum
              val totalMWH = "%.1f".format(totalKWH / 1000)

              println(s"Total Production: $totalMWH MWh")
              println("\nDetailed Records:")

              av01Data.foreach { d =>
                val mwhValue = "%.3f".format(d.value / 1000)
                val localTime = formatter.format(d.startTime)
                println(s"[$localTime] ${d.value} KWH ($mwhValue MWh)")
              }
            }

          case Left(error) =>
            println(s"Error: ${error}")
        }

      case _ =>
        println("Invalid choice.")
    }


  }

  def getValidChoice(): Int = {  //I am not sure is if this is valid
    while (true) {
      println("Please enter your choice:")
      val input = readLine()
      Try(input.toInt).toOption match {
        case Some(num) if num >= 1 && num <= 3 =>
          return num
        case _ =>
          println("Invalid input.")
          return 0
      }
    }
    throw new RuntimeException("unreachable code")
  }
}
