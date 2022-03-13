import helper.FileHelper
import helper.HtmlHelper
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
        println(firstRowResponses)
        if (firstRowResponses.size < 3) {
            client.close()
            continue
        }

        val url = firstRowResponses[1].removePrefix("/")
        val file = File(rootDir, "\\$url")
        println(url)

//        println("= = = = = request from client = = = = =")
//        print(header)
//        println("= = = = = end of request = = = = =")

        val responseHeader: String
        val mimeType: String
        when {
            file.exists() && file.isFile -> {
                mimeType = FileHelper.getMimeType(file)
//                println("absolutePath ${file.absolutePath}")
//                println("mimeType: $mimeType")
                responseHeader = responseHeaderBuilder(
                    code = 200,
                    status = "OK",
                    contentType = mimeType,
                    contentLength = file.length(),
                )

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
                val fileDataList = FileHelper.getAllFileAndDir(file)
                val content: String

                mimeType = "text/html; charset=UTF-8"
                val indexFileData = fileDataList.find {
                    it.name == "index.html" ||
                    it.name == "index.php"
                }

                responseHeader = if (indexFileData != null && !indexFileData.isDirectory) {
                    val indexFile = File(indexFileData.absolutePath)
                    var temp = ""
                    indexFile.readLines().forEach {
                        temp += "$it\n"
                    }
                    content = temp
                    responseHeaderBuilder(
                        code = 302,
                        status = "OK",
                        contentType = mimeType,
                        contentLength = content.length.toLong(),
                        location = "$url/${indexFileData.name}"
                    )
                } else {
                    content = HtmlHelper.generateListingHtml(file, fileDataList)
                    responseHeaderBuilder(
                        code = 200,
                        status = "OK",
                        contentType = mimeType,
                        contentLength = content.length.toLong(),
                    )
                }

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
                val content = "<!doctype html><html><h1>hello mom!</h1><h3>404 Not found, too bad.</h3></html>\r\n"
                mimeType = "text/html; charset=UTF-8"
                responseHeader = responseHeaderBuilder(
                    code = 404,
                    status = "Not found",
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
        client.close()

//        println("= = = = = header from server = = = = =")
//        print(responseHeader)
//        println("= = = = = end of header = = = = =")
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
    location: String = ""
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
    response += if (location.isNotBlank()) {
        "Location: /$location\r\n"
    } else {
        ""
    }

    response += "Content-Type: $contentType\r\n"
    response += "Content-Length: $contentLength\r\n"
    response += "Server: progjarx/v2.0\r\n"
    response += "\r\n"

    return response
}