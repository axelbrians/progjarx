package data

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class FileData(
    val name: String,
    val isDirectory: Boolean,
    val lastModified: String,
    val size: String,
    val absolutePath: String
) {
    companion object {
        val SDF: SimpleDateFormat = SimpleDateFormat("hh:mm dd/MM/yyyy")
    }
}

fun File.toFileData(): FileData {
    val calendar = Calendar.getInstance().also {
        it.timeInMillis = lastModified()
    }
    val fileName = name
    val lastModified = FileData.SDF.format(calendar.time)
    var size = "${length()} B"
    if (length() / 1_000 > 1) {
        size = "${length().toDouble() / 1000} KB"
    }

    if (length() / (1_000_000) > 1) {
        size = "${length().toDouble() / (1_000_000)} MB"
    }

    if (length() / (1000_000_000) > 1) {
        size = "${length().toDouble() / (1000_000_000)} GB"
    }

    return FileData(
        name = fileName,
        isDirectory = this.isDirectory,
        lastModified = lastModified,
        size = size,
        absolutePath = absolutePath
    )
}