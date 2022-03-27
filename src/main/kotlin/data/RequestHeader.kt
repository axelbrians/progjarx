package data

data class RequestHeader(
    val firstRowResponses: List<String>,
    val host: String = "",
    val keepAlive: Boolean = false,
    val range: String = ""
)
