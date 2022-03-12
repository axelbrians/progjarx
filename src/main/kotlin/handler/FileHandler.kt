package handler

import java.io.File
import java.lang.NullPointerException
import java.nio.file.Files

object FileHandler {

    fun getMimeType(url: String): String {
        val file = File(url)
        return Files.probeContentType(file.toPath())
    }

    fun getMimeType(parent: String, child: String): String {
        val file = File(parent, child)
        return Files.probeContentType(file.toPath())
    }

    fun getMimeType(file: File): String {
        return try {
            Files.probeContentType(file.toPath())
        } catch (e: Exception) {
            when (e) {
                is NullPointerException -> {
                    resolveMimeTypeManual(file)
                }
                else -> {
                    ""
                }
            }
        }
    }

    private fun resolveMimeTypeManual(file: File): String {
        return ""
    }

}