package helper

import rootDir
import data.FileData
import java.io.File

object HtmlHelper {

    private const val HTML_WRAPPER =
        "<!DOCTYPE HTML>\n" +
        "<html>\n" +
        "<head>\n%s\n</head>\n" +
        "<body>\n%s\n</body>\n" +
        "</html>"

    private const val HEAD_TITLE =
        "<title>%s</title>\n"

    private const val H1_TAG =
        "<h1>%s</h1>\n"

    private const val TABLE_TAG =
        "<table>\n%s\n</table>\n"

    private const val TR_TAG =
        "<tr>\n%s\n</tr>\n"

    private const val TH_TAG =
        "<th %s>\n%s\n</th>\n"

    private const val TD_TAG =
        "<td %s>%s</td>\n"

    private const val A_TAG =
        "<a %s>%s</a>\n"

    private const val IMG_TAG =
        "<img %s/>"

    fun generateListingHtml(parent: File, fileDataList: List<FileData>): String {
        val workingDir = getWorkingDirectory(parent)
        val headTitle = HEAD_TITLE.format(workingDir)
        val bodyTitle = H1_TAG.format(workingDir)
        var bodyTableContent = ""

        bodyTableContent += createTrTitle()
        bodyTableContent += createTrBorder()
        fileDataList.forEach {
            bodyTableContent += createTrContent(workingDir, it)
        }
        bodyTableContent += createTrBorder()
        val bodyTable = TABLE_TAG.format("$bodyTitle\n$bodyTableContent")

        return HTML_WRAPPER.format(headTitle, bodyTable)
    }

    private fun createTrContent(parentPath: String, file: FileData): String {
        var trContent = ""

        trContent += if (file.isDirectory) {
            TD_TAG.format(
                "valign=\"top\"",
                IMG_TAG.format("src=\"/assets/folder.gif\" alt=\"[ICO]\"")
            )
        } else {
            TD_TAG.format(
                "valign=\"top\"",
                IMG_TAG.format("src=\"/assets/unknown.gif\" alt=\"[ICO]\"")
            )
        }

        trContent += TD_TAG.format(
            "",
            A_TAG.format("href=\"$parentPath/${file.name}\"", file.name)
        )

        trContent += TD_TAG.format(
            "",
            A_TAG.format("align=\"right\"", file.lastModified)
        )

        trContent += if (file.isDirectory) {
            TD_TAG.format(
                "",
                A_TAG.format("align=\"right\"", "-")
            )
        } else {
            TD_TAG.format(
                "",
                A_TAG.format("align=\"right\"", file.size)
            )
        }
        //TODO FIle size
        trContent += TD_TAG.format(
            "",
            A_TAG.format("", "&nbsp;")
        )

        return TR_TAG.format(trContent)
    }

    private fun createTrBorder(): String {
        return TR_TAG.format(TH_TAG.format("colspan=5", "<hr>"))
    }

    private fun createTrTitle(): String {
        var trContent = ""

        trContent += TH_TAG.format(
            "valign=\"top\"",
            IMG_TAG.format("alt=\"[ICO]\"")
        )

        trContent += TH_TAG.format(
            "",
            A_TAG.format("href=\"#\"", "Name")
        )

        trContent += TH_TAG.format(
            "",
            A_TAG.format("href=\"#\"", "Last modified")
        )

        trContent += TH_TAG.format(
            "",
            A_TAG.format("href=\"#\"", "Size")
        )

        trContent += TH_TAG.format(
            "",
            A_TAG.format("href=\"#\"", "Description")
        )

        return TR_TAG.format(trContent)
    }

    private fun getWorkingDirectory(file: File): String {
        return file.path.removePrefix(rootDir).removePrefix("\\")
    }
}