import data.RequestHeaderData
import data.ResponseHeaderData
import helper.FileHelper
import helper.HtmlHelper
import kotlinx.coroutines.*
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class ProgjarXServer(
    private val config: List<String>
) {

    private var staticRootDir: String = System.getProperty("user.dir")
    private val clients = mutableListOf<Socket>()
    private var jobs = MutableList<Job?>(100) { null }
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun serve() {
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
//                try {
                    handleClient(config, client, jobIndex)
//                } catch (exception: Exception) {
//                    println("Internal server error ${exception.message}")
//                }
            }

            jobs[jobIndex] = job

        }
    }

    private fun handleClient(config: List<String>, client: Socket, jobIndex: Int) {
        var rootDir: String = staticRootDir
        val br = BufferedReader(InputStreamReader(client.getInputStream()))
        val bw = BufferedWriter(OutputStreamWriter(client.getOutputStream()))
        val bos = BufferedOutputStream(client.getOutputStream())

        val requestHeader = RequestHeaderData.parseHeader(br)
        val path: String

        for (i in 1 until config.size) {
//            println("config row: " + config[i] + " host = " + config[i].substringBefore(':') + "HOST = " + host)
            if(config[i].substringBefore(':') == requestHeader.host) {
                path = config[i].substringAfter(":./")
                rootDir += "\\$path"
                println("RootDir: $rootDir")
                break
            }
        }


        if (requestHeader.firstRowResponses.size < 3) {
            client.close()
            return
        }

        val url = requestHeader.firstRowResponses[1].removePrefix("/")
        val file = File(rootDir, "\\$url")
        println("relativeUrl $url")

//        println("= = = = = request from client = = = = =")
//        print(header)
//        println("= = = = = end of request = = = = =")

        val responseHeader: ResponseHeaderData
        val mimeType: String
        when {
            file.exists() && file.isFile -> {
//            println("accessing ${file.path}")
                mimeType = FileHelper.getMimeType(file)
//                println("absolutePath ${file.absolutePath}")
//                println("mimeType: $mimeType")
                responseHeader = if(requestHeader.range.isBlank()) {
                     ResponseHeaderData(
                        statusCode = 200,
                        statusMessage = "OK",
                        contentType = mimeType,
                        contentLength = file.length(),
                    )
                } else {
                    ResponseHeaderData(
                        statusCode = 206,
                        statusMessage = "Partial Content",
                        contentType = mimeType,
                        contentLength = file.length(),
                        contentRange = requestHeader.range
                    )
                }


                with(bw) {
                    try{
                        write(responseHeader.toString())
                        flush()
                    }
                    catch (e: Exception) {
                        println(e)
                    }

                }
                file.inputStream().use {
                    try {
                        val byteArray = ByteArray(1024 * 8)
                        var read: Int = 0
                        if(requestHeader.range.isBlank()) {
                            read = it.read(byteArray)
                            while (true) {
                                bos.write(byteArray, 0, read)
                                read = it.read(byteArray)
                                if (read < 0) {
                                    break
                                }
                            }
                        }
                        else {
                            it.skip(requestHeader.rangeStart)
                            if(requestHeader.rangeEnd == "-1".toLong()) {
                                requestHeader.rangeEnd = file.length()
                            }
                            var rangeSize = requestHeader.rangeEnd - requestHeader.rangeStart
//                            println("RangeEnd = ${requestHeader.rangeEnd}\nRange Size = $rangeSize")
                            read = it.read(byteArray)
                            rangeSize -= read
                            while (true) {
                                bos.write(byteArray, 0, read)
                                read = it.read(byteArray)
                                rangeSize -= read
                                if(rangeSize < 0) {
                                    break
                                }
                                if (read < 0) {
                                    break
                                }
                            }
                        }
                        bos.flush()
                    }
                    catch (e: Exception) {
                        println(e)
                    }
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
                    var location = "http://${requestHeader.host}"
                    if (url.isNotBlank()) {
                        location += "/$url"
                    }
                    location += "/${indexFileData.name}"
                    ResponseHeaderData(
                        statusCode = 302,
                        statusMessage = "OK",
                        contentType = mimeType,
                        contentLength = content.length.toLong(),
                        location = location
                    )
                } else {
                    content = HtmlHelper.generateListingHtml(file, fileDataList, rootDir)
                    ResponseHeaderData(
                        statusCode = 200,
                        statusMessage = "OK",
                        contentType = mimeType,
                        contentLength = content.length.toLong(),
                    )
                }

                with(bw) {
                    write(responseHeader.toString())
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
                responseHeader = ResponseHeaderData(
                    statusCode = 404,
                    statusMessage = "Not found",
                    contentType = mimeType,
                    contentLength = content.length.toLong(),
                )
                with(bw) {
                    write(responseHeader.toString())
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
}