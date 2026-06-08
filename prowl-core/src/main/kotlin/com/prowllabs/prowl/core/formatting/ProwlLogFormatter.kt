package com.prowllabs.prowl.core.formatting

import com.prowllabs.prowl.core.model.NetworkLog
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Date

enum class ProwlExportFormat(val fileName: String) {
    FORMATTED_TEXT("prowl-logs.txt"),
    CURL_COMMANDS("prowl-curl.sh"),
}

object ProwlLogFormatter {
    fun export(logs: List<NetworkLog>, format: ProwlExportFormat): String =
        when (format) {
            ProwlExportFormat.FORMATTED_TEXT -> formattedText(logs)
            ProwlExportFormat.CURL_COMMANDS -> curlBundle(logs)
        }

    fun shareText(log: NetworkLog): String = formattedText(listOf(log))

    fun bodyText(body: NetworkLog.Body, pretty: Boolean = true): String =
        if (pretty) prettyBodyText(body) else body.data.toString(Charsets.UTF_8)

    fun prettyBodyText(body: NetworkLog.Body): String {
        val raw = body.data.toString(Charsets.UTF_8)
        runCatching { JSONObject(raw) }.getOrNull()?.let {
            return it.toString(2)
        }
        runCatching { JSONArray(raw) }.getOrNull()?.let {
            return it.toString(2)
        }
        return raw.ifBlank { body.data.joinToString("") { byte -> "%02x".format(byte) } }
    }

    private fun formattedText(logs: List<NetworkLog>): String =
        logs.joinToString("\n\n${"=".repeat(60)}\n\n") { log ->
            buildString {
                appendLine("*INFO*")
                appendLine("URL: ${log.url ?: "-"}")
                appendLine("Method: ${log.method}")
                appendLine("Status: ${log.statusCode ?: "N/A"}")
                appendLine("Started: ${Date(log.startedAtMillis)}")
                appendLine("Duration: ${log.durationMillis}ms")
                appendLine()
                appendLine("*REQUEST*")
                log.requestHeaders.toSortedMap(String.CASE_INSENSITIVE_ORDER).forEach { (k, v) ->
                    appendLine("$k: $v")
                }
                log.requestBody?.let {
                    appendLine()
                    appendLine(prettyBodyText(it))
                }
                appendLine()
                appendLine("*RESPONSE*")
                log.responseHeaders.toSortedMap(String.CASE_INSENSITIVE_ORDER).forEach { (k, v) ->
                    appendLine("$k: $v")
                }
                log.responseBody?.let {
                    appendLine()
                    appendLine(prettyBodyText(it))
                }
            }
        }

    private fun curlBundle(logs: List<NetworkLog>): String =
        logs.joinToString("\n\n") { log -> curlCommand(log) }

    fun curlCommand(log: NetworkLog): String {
        val url = log.url ?: return "# missing url"
        val builder = StringBuilder("curl -X ${log.method.uppercase()} ")
        log.requestHeaders.forEach { (key, value) ->
            builder.append("-H '${shellEscape("$key: $value")}' ")
        }
        log.requestBody?.data?.takeIf { it.isNotEmpty() }?.let { body ->
            val text = body.toString(Charsets.UTF_8)
            builder.append("-d '${shellEscape(text)}' ")
        }
        builder.append("'${shellEscape(url)}'")
        return builder.toString().trim()
    }

    private fun shellEscape(value: String): String =
        value.replace("'", "'\\''")
}
