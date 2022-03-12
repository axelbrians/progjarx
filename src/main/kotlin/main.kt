import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.*
import java.net.ServerSocket

val rootDir = "${System.getProperty("user.dir")}\\src\\main\\kotlin"

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
        val url = header.substringBefore("\n", "")

        val file = File(rootDir, "\\index.html")
        println("= = = = = request from client = = = = =")
        print(header)
        println("= = = = = end of request = = = = =")
        val dummy = "<!doctype html><html><p>hello mom!</p></html>"
        var response = responseBuilder(
            200,
            "OK",
            "OK",
            "text/html; charset=UTF-8",
            dummy.length.toLong()
        )

        response += dummy + "\r\n"
        println("= = = = = response from server = = = = =")
        print(response)
        println("= = = = = end of response = = = = =")
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
    status: String,
    message: String
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
    message: String,
    contentType: String,
    contentLength: Long
): String {
    var response = ""

    response += "HTTP/1.1"
    response += " $code"
    response += " $status\r\n"
    response += "Content-Type: $contentType\r\n"
    response += "Content-Length: $contentLength\r\n"
    response += "Server: progjarx\r\n"
    response += "\r\n"

    return response
}