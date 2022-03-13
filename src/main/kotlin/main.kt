import helper.FileHelper
import kotlinx.coroutines.runBlocking
import java.io.*
import java.net.ServerSocket
import java.text.SimpleDateFormat
import java.util.*

val rootDir: String = System.getProperty("user.dir")

fun main() = runBlocking {
    val inputStream: InputStream = File("$rootDir\\progjarx.conf").inputStream()
//    val serverPort = 80
    val config = mutableListOf<String>()
    inputStream.bufferedReader().useLines { line ->
        line.forEach { config.add(it)}
    }
    val serverPort = config[0].substringAfter(':').toInt()
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

        println("= = = = = request from client = = = = =")
        print(header)
        println("= = = = = end of request = = = = =")

        val responseHeader: String
        val mimeType: String
        when {
            file.exists() && file.isFile -> {
                mimeType = FileHelper.getMimeType(file)
                responseHeader = responseHeaderBuilder(
                    code = 200,
                    status = "OK",
                    contentType = mimeType,
                    contentLength = file.length(),
                )
                val sdf = SimpleDateFormat("hh:mm dd/MM/yyyy")
                val calendar = Calendar.getInstance().also {
                    it.timeInMillis = file.lastModified()
                }
                val lastModified = sdf.format(calendar.time)


                println("fileName ${file.name}")
                println("size ${file.length()}")
                println("lastModified $lastModified")


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
            }
            file.exists() && file.isDirectory -> {
                val content = "<!doctype html><html><h1>listing directories WIP</h1></html>\n"
                mimeType = "text/html; charset=UTF-8"
                responseHeader = responseHeaderBuilder(
                    code = 200,
                    status = "OK",
                    contentType = mimeType,
                    contentLength = content.length.toLong(),
                )
                with(bw) {
                    write(responseHeader)
                    flush()
                }
                with(bos) {
                    write(content.toByteArray(), 0, content.length)
                    flush()
                }
            }
            else -> {
                val content = "<!doctype html><html><p>hello mom!</p></html>\r\n"
                mimeType = "text/html; charset=UTF-8"
                responseHeader = responseHeaderBuilder(
                    code = 200,
                    status = "OK",
                    contentType = mimeType,
                    contentLength = content.length.toLong(),
                )
                with(bw) {
                    write(responseHeader)
                    flush()
                }
                with(bos) {
                    write(content.toByteArray(), 0, content.length)
                    flush()
                }
            }
        }

        println("= = = = = header from server = = = = =")
        print(responseHeader)
        println("= = = = = end of header = = = = =")
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