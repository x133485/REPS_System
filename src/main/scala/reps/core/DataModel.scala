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

  case class WindSolarMetrics(
                          startTime: Instant,
                          endTime: Instant,
                          powerOutput: Double
                        )

  object WindSolarMetrics {
    implicit val rw: ReadWriter[WindSolarMetrics] = macroRW
  }
  
}
