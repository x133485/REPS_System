package reps.io

import reps.core.DataModel
import _root_.upickle.default.{ReadWriter, read, macroRW}
import sttp.client3._
import java.time.Instant
import javax.net.ssl.{SSLContext, TrustManager, X509TrustManager}


object ApiDataFetcher {


  private val trustAllCerts: Array[TrustManager] =
    Array(new X509TrustManager {
      def getAcceptedIssuers: Array[java.security.cert.X509Certificate] = null
      def checkClientTrusted(certs: Array[java.security.cert.X509Certificate], authType: String): Unit = ()
      def checkServerTrusted(certs: Array[java.security.cert.X509Certificate], authType: String): Unit = ()
    })


  private val sslContext: SSLContext = {
    val ctx = SSLContext.getInstance("TLS")
    ctx.init(null, trustAllCerts, new java.security.SecureRandom())
    ctx
  }


  private val backend: SttpBackend[Identity, Any] =
    HttpURLConnectionBackend(
      options = SttpBackendOptions.Default,
      customizeConnection = {
        case https: javax.net.ssl.HttpsURLConnection =>
          https.setSSLSocketFactory(sslContext.getSocketFactory)
          https.setHostnameVerifier((_, _) => true)
        case _ => // 非 HTTPS 连接无需处理
      }
    )

  /* ------------------------------------------------- *
  * 2. General data fetching method
  * ------------------------------------------------- */

  /**
   * Call the specified URL and parse it into List[T]
   *
   * @param url       Fingrid dataset URL
   * @param apiKey    Personal API Key
   * @param startTime Start time
   * @param endTime   End time
   * @param parseFunc Function to convert RawApiResponse to target type T
   */
  def fetchData[T: ReadWriter](
                                url: String,
                                apiKey: String,
                                startTime: Instant,
                                endTime: Instant,
                                parseFunc: RawApiResponse => T
                              ): Either[String, List[T]] =
    try {
      val request = basicRequest
        .get(uri"$url?startTime=$startTime&endTime=$endTime")
        .header("x-api-key", apiKey)

      val response = request.send(backend)

      response.body match
        case Right(rawJson) if response.code.isSuccess =>
          parseJsonResponse(rawJson, parseFunc)
        case Right(_)  => Left(s"API Error: ${response.code}")
        case Left(err) => Left(s"HTTP Error: $err")
    } catch {
      case e: Exception => Left(s"Request Failed: ${e.getMessage}")
    }

  /* ------------------------------------------------- *
   * 3.   JSON ↔  case class
   * ------------------------------------------------- */

  case class RawApiResponse(
                             startTime: String,
                             endTime: String,
                             value: Double,
                             additionalJson: Option[Map[String, String]] = None
                           )
  object RawApiResponse {
    implicit val rw: ReadWriter[RawApiResponse] = macroRW
  }

  private def parseJsonResponse[T: ReadWriter](
                                                json: String,
                                                parseFunc: RawApiResponse => T
                                              ): Either[String, List[T]] =
    try {
      val rawArr = ujson.read(json)("data").arr
      val result = rawArr
        .map(item => read[RawApiResponse](item))
        .map(parseFunc)
        .toList
      Right(result)
    } catch {
      case e: Exception => Left(s"JSON parsing failed: ${e.getMessage}")
    }
}
