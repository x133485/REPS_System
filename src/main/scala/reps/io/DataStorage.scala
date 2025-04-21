package reps.io

import reps.core.DataModel
import java.nio.file.{Files, Paths}

object CSVDataStorage {
  def saveToCSV(data: List[DataModel.WindSolarMetrics], path: String): Unit = {
    val lines = "startTime,endTime,powerOutput" :: data.map(d =>
      s"${d.startTime},${d.endTime},${d.powerOutput}"
    )
    Files.write(Paths.get(path), lines.mkString("\n").getBytes)
  }
}
