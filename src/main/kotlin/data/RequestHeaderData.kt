package data

import java.io.BufferedReader

data class RequestHeaderData(
    val firstRowResponses: List<String>,
    val host: String = "",
    val keepAlive: Boolean = false,
    val range: String = "",
    val rangeStart: Long,
    val rangeEnd: Long
) {
    companion object {
        fun parseHeader(
            br: BufferedReader
        ): RequestHeaderData {
            var header = ""
            var host = ""
            var keepAlive = false
            var range = ""
            var rangeStart: Long = -1
            var rangeEnd: Long = -1
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
                        range = message.substringAfter(": ")
                        var valRange  = range.substringAfter("bytes=")
                        rangeStart = valRange.substringBefore('-',"-1").toLong()
                        rangeEnd = valRange.substringAfter('-',"-1").toLong()
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
            println("Range Start: $rangeStart")
            println("Range End: $rangeEnd")
            return RequestHeaderData(
                firstRowResponses = firstRowResponse,
                host = host,
                keepAlive = keepAlive,
                range = range,
                rangeStart = rangeStart,
                rangeEnd = rangeEnd
            )
        }
    }
}
