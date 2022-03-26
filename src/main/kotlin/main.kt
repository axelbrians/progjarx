import helper.FileHelper
import helper.HtmlHelper
import kotlinx.coroutines.*
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

var staticRootDir: String = System.getProperty("user.dir")
var host: String? = ""
val ttl = 100
val timeout = 15
var keepAlive = false

private val clients = mutableListOf<Socket>()
private var jobs = MutableList<Job?>(100) { null }
private val coroutineScope = CoroutineScope(Dispatchers.Default)

fun main() {

    val inputStream: InputStream = File("$staticRootDir\\progjarx.conf").inputStream()
//    val serverPort = 80

    val config = mutableListOf<String>()
    inputStream.bufferedReader().useLines { line ->
        line.forEach { config.add(it)}
    }

    val serverIP = config[0].substringBefore(':')
    val serverPort = config[0].substringAfter(':').toInt()
    val server = ServerSocket(serverPort, 5, InetAddress.getByName(serverIP))
    println("Server is active, listening at port: $serverPort")
    while (true) {
        println("= = = = = Waiting for next client to connect = = = = =")
        val client = server.accept()

        clients.add(client)

        var jobIndex = jobs.indexOfFirst { it == null }
        if (jobIndex < 0 || jobIndex > jobs.size - 1) {
            while (true) {
                jobIndex = jobs.indexOfFirst { it == null }
                if (jobIndex >= 0 && jobIndex < jobs.size) {
                    break
                }
            }
        }
        val job = coroutineScope.launch {
//            println("= = = = = client $jobIndex connected to ${client.inetAddress} = = = = =")
            try {
                handleClient(config, client, jobIndex)
            } catch (exception: Exception) {
                println("Internal server error ${exception.message}")
            }
        }

        jobs[jobIndex] = job

    }

}
//catch (e: Exception) {
//    print("Server crashed with ${e.message}")
//}

fun handleClient(config: List<String>, client: Socket, jobIndex: Int) {
    var rootDir: String = staticRootDir
    val br = BufferedReader(InputStreamReader(client.getInputStream()))
    val bw = BufferedWriter(OutputStreamWriter(client.getOutputStream()))
    val bos = BufferedOutputStream(client.getOutputStream())

    val header = getRequestHeader(br)

    val path: String
    for (i in 1 until config.size) {
//            println("config row: " + config[i] + " host = " + config[i].substringBefore(':') + "HOST = " + host)
        if(config[i].substringBefore(':') == host) {
            path = config[i].substringAfter(":./")
            rootDir += "\\$path"
            break
        }
    }

    val firstRowResponses = header.substringBefore("\n", "").split(" ")
    println(firstRowResponses)
    if (firstRowResponses.size < 3) {
        client.close()
        return
    }

    val url = firstRowResponses[1].removePrefix("/")
    val file = File(rootDir, "\\$url")
    println("relativeUrl $url")

//        println("= = = = = request from client = = = = =")
//        print(header)
//        println("= = = = = end of request = = = = =")

    val responseHeader: String
    val mimeType: String
    when {
        file.exists() && file.isFile -> {
//            println("accessing ${file.path}")
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
//            println("accessing ${file.path}")
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
                var location = "http://$host"
                if (url.isNotBlank()) {
                    location += "/$url"
                }
                location += "/${indexFileData.name}"
                responseHeaderBuilder(
                    code = 302,
                    status = "OK",
                    contentType = mimeType,
                    contentLength = content.length.toLong(),
                    location = location
                )
            } else {
                content = HtmlHelper.generateListingHtml(file, fileDataList, rootDir)
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
//        if(keepAlive) delay(3000)
    client.close()
    clients.remove(client)
    jobs[jobIndex]?.cancel()
    jobs[jobIndex] = null
//    println(
//        "= = = = = " +
//        "client $jobIndex disconnected, now open for request " +
//        "= = = = ="
//    )
//        println("= = = = = header from server = = = = =")
//        print(responseHeader)
//        println("= = = = = end of header = = = = =")
}

fun getRequestHeader(br: BufferedReader): String {
    var header = ""

    while (true) {
        val message: String? = br.readLine()
//        println(message)
        if(message != null && message.contains("Host")) {
            host = message.substringAfter(' ')
        }
        if(message != null && message.contains("keep-alive")) {
            keepAlive = true
        }
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
        "Location: $location\r\n"
    } else {
        ""
    }

    response += "Content-Type: $contentType\r\n"
    response += "Content-Length: $contentLength\r\n"
    if(keepAlive) {
        response += "Keep-Alive: timeout=$timeout, max=$ttl\r\n"
        response +="Connection: Keep-Alive\r\n"
    }
    response += "Server: progjarx/v2.0\r\n"
    response += "\r\n"

    return response
}