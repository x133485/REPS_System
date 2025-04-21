package reps.core

import java.time.Instant
import upickle.default.{ReadWriter, readwriter, StringReader, StringWriter, macroRW}

object DataModel {

  implicit val instantRW: ReadWriter[Instant] =
    readwriter[String].bimap[Instant](
      instant => instant.toString,
      str => Instant.parse(str)
    )

  case class PowerForecast(
    startTime: Instant,
    endTime: Instant,
    powerOutput:Double)

  object PowerForecast{
    implicit val rw: ReadWriter[PowerForecast] = macroRW
  }

  case class WindMetrics(
                          startTime: Instant,
                          endTime: Instant,
                          powerOutput: Double
                        )

  object WindMetrics {
    implicit val rw: ReadWriter[WindMetrics] = macroRW
  }

  case class HydropowerData(
                             startTime: Instant,
                             endTime: Instant,
                             productionType: String,
                             value: Double
                           )

  object HydropowerData {
    implicit val rw: ReadWriter[HydropowerData] = macroRW
  }
}
