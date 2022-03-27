import java.io.File
import java.io.InputStream

fun main() {

    val staticRootDir: String = System.getProperty("user.dir")
    val inputStream: InputStream = File("$staticRootDir\\progjarx.conf").inputStream()

    val config = mutableListOf<String>()
    inputStream.bufferedReader().useLines { line ->
        line.forEach { config.add(it) }
    }

    with(ProgjarXServer(config)) {
        serve()
    }
}