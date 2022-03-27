package data

import java.text.SimpleDateFormat
import java.util.*

data class ResponseHeaderData(
    val httpVersion: String = "HTTP/1.1",
    val statusCode: Int,
    val statusMessage: String,
    val contentType: String,
    val contentLength: Long,
    val contentDisposition: String? = null,
    val contentRange: String? = null,
    val connection: String? = null,
    val location: String? = null,
    val date: String? = null
) {
    companion object {
        private val timeZone = TimeZone.getTimeZone("GMT")
        private val sdf = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z").apply {
            this.timeZone = ResponseHeaderData.timeZone
        }

    }

    override fun toString(): String {
        var response = ""

        response += "HTTP/1.1"
        response += " $statusCode"
        response += " $statusMessage\r\n"

        response += if (date != null) {
            "Date: $date\r\n"
        } else {
            val timeInMillis = System.currentTimeMillis()
            val calendar = Calendar.getInstance().apply {
                this.timeInMillis = timeInMillis
            }
            "Date: ${sdf.format(calendar.time)}\r\n"
        }

        response += if (contentType.contains("text")) {
            "Content-Disposition: inline\r\n"
        } else {
            "Content-Disposition: attachment\r\n"
        }
        response += if (location?.isNotBlank() == true) {
            "Location: $location\r\n"
        } else {
            ""
        }

        response += "Content-Type: $contentType\r\n"
        response += "Content-Length: $contentLength\r\n"
        if(contentRange != null) {
            response += "Accept-Ranges: bytes\r\n"
            response += "Content-Range: $contentRange/$contentLength\r\n"
        }
        if(connection != null) {
            response += "Connection: $connection\r\n"
            response += "Keep-Alive: timeout=5, max=100\r\n"
        } else {
            response += "Connection: close\r\n"
        }
        response += "Server: progjarx/v2.0\r\n"
        response += "\r\n"

        println("= = = = = = = = Respons lh = = = = = = = = = =")
        println(response)
        return response
    }
}
