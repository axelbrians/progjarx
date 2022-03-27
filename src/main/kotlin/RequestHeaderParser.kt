import data.RequestHeader
import java.io.BufferedReader

object RequestHeaderParser {

    fun parseHeader(
        br: BufferedReader
    ): RequestHeader {
        var header = ""
        var host = ""
        var keepAlive = false
        var range = ""
        try {
            while (true) {
                val message: String? = br.readLine()
                if(message != null && message.contains("Host")) {
                    host = message.substringAfter(' ')
                }
                if(message != null && message.contains("keep-alive")) {
                    keepAlive = true
                }
                if(message != null && message.contains("Range: ")) {
                    range = message
                }
                if (message == null || message == "\r\n" || message.isBlank()) {
                    break
                }
                header += "$message\n"
            }
        } catch (e: Exception) {
            println(e)
        }
        val firstRowResponse = header.substringBefore("\n", "").split(" ")
        return RequestHeader(
            firstRowResponses = firstRowResponse,
            host = host,
            keepAlive = keepAlive,
            range = range
        )
    }
}