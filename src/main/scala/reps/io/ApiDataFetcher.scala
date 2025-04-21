package reps.io

import _root_.upickle.default.{ReadWriter, read, macroRW}
import sttp.client3._
import cats.implicits._
import java.time.Instant

/**
 * Generic Fingrid‑API fetcher (no custom SSL handling; relies on default JVM trust store).
 */
object ApiDataFetcher {

  /* ---------- Backend ---------- */
  private val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

  /* ---------- Public fetch ---------- */
  def fetchData[T: ReadWriter](
                                url: String,
                                apiKey: String,
                                start: Instant,
                                end: Instant,
                                parse: Raw => T
                              ): Either[String, List[T]] =
    try {
      val res = basicRequest
        .get(uri"$url?startTime=$start&endTime=$end")
        .header("x-api-key", apiKey)
        .send(backend)

      res.body match
        case Right(json) if res.code.isSuccess => parseJson(json, parse)
        case Right(_) => Left(s"API status ${res.code}")
        case Left(err) => Left(s"HTTP error $err")
    } catch
      case e: Exception => Left(s"Request failed: ${e.getMessage}")

  /* ---------- JSON ↔ Case class ---------- */
  case class Raw(startTime: String, endTime: String, value: Double)

  object Raw {
    implicit val rw: ReadWriter[Raw] = macroRW
  }

  private def parseJson[T: ReadWriter](json: String, f: Raw => T): Either[String, List[T]] =
    try
      Right(
        ujson.read(json)("data").arr
          .map(v=> read[Raw](v))
          .map(f)
          .toList
      )
    catch
      case e: Exception => Left(s"JSON parse error: ${e.getMessage}")
}

