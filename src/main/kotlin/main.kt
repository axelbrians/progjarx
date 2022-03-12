import handler.FileHandler
import kotlinx.coroutines.delay
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

        val header = getRequestHeader(br)
        val firstRowResponses = header.substringBefore("\n", "").split(" ")
        val url = firstRowResponses[1].removePrefix("/")
        val file = File(rootDir, "\\$url")

        println(file.path)
        println(file.toPath())

        if (file.exists() && file.isFile) {
            val mimeType = FileHandler.getMimeType(file)
            println("mimeType: $mimeType")
        }



        println("= = = = = request from client = = = = =")
        print(header)
        println("= = = = = end of request = = = = =")
        val dummy = "<!doctype html><html><p>hello mom!</p></html>"
        var response = responseBuilder(
            code = 200,
            status = "OK",
            contentType = "text/html; charset=UTF-8",
            contentLength = dummy.length.toLong(),
            content = dummy
        )

        response += dummy + "\r\n"
//        println("= = = = = response from server = = = = =")
//        print(response)
//        println("= = = = = end of response = = = = =")
        with(bw) {
            write(response)
            flush()
        }
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

fun responseBuilder(
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

fun responseBuilder(
    code: Int,
    status: String,
    contentType: String,
    contentLength: Long,
    content: Any? = ""
): String {
    var response = ""

    response += "HTTP/1.1"
    response += " $code"
    response += " $status\r\n"
    response += "Content-Type: $contentType\r\n"
    response += "Content-Length: $contentLength\r\n"
    response += "Server: progjarx\r\n"
    response += "\r\n"
    response += content.toString()

    return response
}