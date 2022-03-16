import helper.FileHelper
import helper.HtmlHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.*
import java.net.*
import java.util.*

var staticRootDir: String = System.getProperty("user.dir")
var rootDir: String = staticRootDir
var host: String? = ""
const val max = 100
const val timeout = 5
var keepAlive = false
var isSent = false

fun main() = runBlocking {
    val inputStream: InputStream = File("$rootDir\\progjarx.conf").inputStream()
//    val serverPort = 80

    val config = mutableListOf<String>()
    inputStream.bufferedReader().useLines { line ->
        line.forEach { config.add(it)}
    }

    val serverIP = config[0].substringBefore(':')
    val serverPort = config[0].substringAfter(':').toInt()
    val server = ServerSocket()
    val endPoint = InetSocketAddress(InetAddress.getByName(serverIP), serverPort)
    server.bind(endPoint)
    println("Server is active, listening at port: $serverPort")
    while (true) {
        println("= = = = = Waiting for next client to connect = = = = =")
        val client = server.accept()
//        println("= = = = = connected to $host = = = = =")

        keepAlive = false

        var br = BufferedReader(InputStreamReader(client.getInputStream()))
        var bw = BufferedWriter(OutputStreamWriter(client.getOutputStream()))
        var bos = BufferedOutputStream(client.getOutputStream())

        var header = getRequestHeader(br, client)

        rootDir = staticRootDir

        var path: String
        for (i in 1 until config.size) {
//            println("config row: " + config[i] + " host = " + config[i].substringBefore(':') + "HOST = " + host)
            if(config[i].substringBefore(':') == host) {
                path = config[i].substringAfter(":./")
                rootDir += "\\$path"
                break
            }
        }

        var firstRowResponses = header.substringBefore("\n", "").split(" ")
//        println(firstRowResponses)
        if (firstRowResponses.size < 3) {
            client.close()
            continue
        }

        var url = firstRowResponses[1].removePrefix("/")
        var file = File(rootDir, "\\$url")
        println("url: $url")

//        println("= = = = = request from client = = = = =")
//        print(header)
//        println("= = = = = end of request = = = = =")

        if(keepAlive) {
            response(file, bw, bos, url)
            println("KeepAlive")

            try {
                while(true) {
//                        if(isSent) {
//                            keepAlive = false
//                            println("\nSending Responsss\n")
                        br = BufferedReader(InputStreamReader(client.getInputStream()))
                        bw = BufferedWriter(OutputStreamWriter(client.getOutputStream()))
                        bos = BufferedOutputStream(client.getOutputStream())
                        header = getRequestHeader(br)
                        rootDir = staticRootDir
                        var path: String
                        for (i in 1 until config.size) {
                            if(config[i].substringBefore(':') == host) {
                                path = config[i].substringAfter(":./")
                                rootDir += "\\$path"
                                break
                            }
                        }
                        if(header == "null") {
//                            println("x")
                            continue
                        }
                        firstRowResponses = header.substringBefore("\n", "").split(" ")
//                        println("Frist RWO RSPONS : " + firstRowResponses)
                        if(firstRowResponses.size < 3) {
                            continue
                        }
                        else {
                            url = firstRowResponses[1].removePrefix("/")
                        }
                        file = File(rootDir, "\\$url")
                        println("url: $url")
//                            isSent = false
                        response(file, bw, bos, url)
//                            println("end of responsss")
//                        }
                }
            }
            catch (e: SocketTimeoutException) {
                println("hello mom! timeout!")
            }
            catch (e: Exception) {
                println("Server error: " + e.message)
            }

        } else {
            response(file, bw, bos, url)
        }

        // Close the connection
        try {
            client.keepAlive = false
            client.tcpNoDelay = false
            client.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        System.out.format("[%s] Closing client access\n", Date())


//        println("= = = = = header from server = = = = =")
//        print(responseHeader)
//        println("= = = = = end of header = = = = =")
    }

}
//catch (e: Exception) {
//    print("Server crashed with ${e.message}")
//}

fun response(file: File, bw: BufferedWriter, bos: BufferedOutputStream, url: String) {
    val responseHeader: String
    val mimeType: String
    when {
        file.exists() && file.isFile -> {
            println("accessing ${file.path}")
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
            println("accessing ${file.path}")
            val fileDataList = FileHelper.getAllFileAndDir(file)
            val content: String

            mimeType = "text/html; charset=UTF-8"
            val indexFileData = fileDataList.find {
                it.name == "index.html"
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
//    println("respons header")
//    println(responseHeader)
//    println("end respons header")
    isSent = true
}

fun getRequestHeader(br: BufferedReader, client: Socket): String {
    var header = ""

    while (true) {
        val message: String? = br.readLine()
//        println(message)
        if(message != null && message.contains("Host")) {
            host = message.substringAfter(' ')
        }
        if(message != null && message.contains("keep-alive")) {
            keepAlive = true
            client.keepAlive = true
            client.tcpNoDelay = true
            client.soTimeout = (timeout * 1000)
            break
        }
        if (message == null || message == "\r\n" || message.isBlank()) {
            break
        }
        header += "$message\n"
    }

//    println("=================HEADER=================")
//    println(header)
//    println("=================END OF HEADER=================")
    return header
}

fun getRequestHeader(br: BufferedReader): String {
    var header = ""

    while (true) {
        val message: String? = br.readLine()

//        println(message)
        if(message != null && message.contains("Host")) {
            host = message.substringAfter(' ')
            break
        }
        if (message == null || message == "\r\n" || message.isBlank()) {
            break
        }
        header += "$message\n"
    }
//    println("=================HEADER=================")
//    println(header)
//    println("=================END OF HEADER=================")
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
        response += "Keep-Alive: timeout=$timeout, max=$max\r\n"
        response +="Connection: Keep-Alive\r\n"
    }
    response += "Server: progjarx/v2.0\r\n"
    response += "\r\n"

    return response
}