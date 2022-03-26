package helper

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

    fun generateListingHtml(
        parent: File,
        fileDataList: List<FileData>,
        rootDir: String
    ): String {
        println("rootDir $rootDir")
        val workingDir = getWorkingDirectory(parent, rootDir)
        var headTitle = HEAD_TITLE.format(workingDir)
        val bodyTitle = H1_TAG.format(workingDir)
        var bodyTableContent = ""

//        println("workingDir $workingDir")
//        println("fileName ${fileDataList.first().absolutePath}")
        headTitle += "<link rel=\"icon\" href=\"http://progjarx.com/assets/progjarx.ico\">"

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
                IMG_TAG.format("src=\"http://progjarx.com/assets/folder.gif\" alt=\"[ICO]\"")
            )
        } else {
            TD_TAG.format(
                "valign=\"top\"",
                IMG_TAG.format("src=\"http://progjarx.com/assets/unknown.gif\" alt=\"[ICO]\"")
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
            IMG_TAG.format("height=\"24px\" src=\"http://progjarx.com/assets/progjarx.ico\" alt=\"[ICO]\"")
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

    private fun getWorkingDirectory(file: File, rootDir: String): String {
        return file.path.substringAfterLast("\\")
    }
}