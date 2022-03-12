import handler.FileHandler
import kotlinx.coroutines.runBlocking
import java.io.*
import java.net.ServerSocket

val rootDir: String = System.getProperty("user.dir")

fun main() = runBlocking {
    val serverPort = 80
    val server = ServerSocket(serverPort)
    println("Server is active, listening at port: $serverPort")
    while (true) {
        println("= = = = = Waiting for next client to connect = = = = =")
        val client = server.accept()
        val br = BufferedReader(InputStreamReader(client.getInputStream()))
        val bw = BufferedWriter(OutputStreamWriter(client.getOutputStream()))
        val bos = BufferedOutputStream(client.getOutputStream())

        val header = getRequestHeader(br)
        val firstRowResponses = header.substringBefore("\n", "").split(" ")
        val url = firstRowResponses[1].removePrefix("/")
        val file = File(rootDir, "\\$url")

        if (file.exists() && file.isFile) {
            val mimeType = FileHandler.getMimeType(file)
            println("mimeType: $mimeType")
            val responseHeader = responseHeaderBuilder(
                    code = 200,
                    status = "OK",
                    contentType = mimeType,
                    contentLength = file.length(),
                )

            println("= = = = = response from server = = = = =")
            print(responseHeader)
            println("= = = = = end of response = = = = =")

            with(bw) {
                write(responseHeader)
                flush()
            }

            file.inputStream().use {
                val byteArray = ByteArray(1024 * 8)
                var read = it.read(byteArray)
                while (true) {
                    bos.write(byteArray, 0, read)
                    read = it.read(byteArray)
                    if (read < 0) {
                        break
                    }
                }
                bos.flush()
            }
        } else {
            val dummy = "<!doctype html><html><p>hello mom!</p></html>"
            var response = responseHeaderBuilder(
                code = 200,
                status = "OK",
                contentType = "text/html; charset=UTF-8",
                contentLength = dummy.length.toLong(),
            )

            println("= = = = = response from server = = = = =")
            print(response)
            println("= = = = = end of response = = = = =")

            response += dummy + "\r\n"
            with(bw) {
                write(response)
                flush()
            }
        }

        println("= = = = = request from client = = = = =")
        print(header)
        println("= = = = = end of request = = = = =")



    }

}
//catch (e: Exception) {
//    print("Server crashed with ${e.message}")
//}

fun getRequestHeader(br: BufferedReader): String {
    var header = ""

    while (true) {
        val message: String? = br.readLine()
//        println(message)
        if (message == null || message == "\r\n" || message.isBlank()) {
            break
        }
        header += "$message\n"
    }

    return header
}

fun responseHeaderBuilder(
    code: Int,
    status: String
): String {
    var response = ""

    response += "HTTP/1.1"
    response += " $code"
    response += " $status\r\n"
    response += "\r\n"

    return response
}

fun responseHeaderBuilder(
    code: Int,
    status: String,
    contentType: String,
    contentLength: Long,
): String {
    var response = ""

    response += "HTTP/1.1"
    response += " $code"
    response += " $status\r\n"

    response += if (contentType.contains("text")) {
        "Content-Disposition: inline\r\n"
    } else {
        "Content-Disposition: attachment\r\n"
    }

    response += "Content-Type: $contentType\r\n"
    response += "Content-Length: $contentLength\r\n"
    response += "Server: progjarx\r\n"
    response += "\r\n"

    return response
}