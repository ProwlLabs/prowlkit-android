package com.prowllabs.prowl.core.formatting

import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.util.BodyDecoder
import com.prowllabs.prowl.core.util.JsonPrettyPrinter
import java.net.URLEncoder
import java.util.Date

enum class ProwlExportFormat(val fileName: String) {
    FORMATTED_TEXT("prowl-logs.txt"),
    CURL_COMMANDS("prowl-curl.sh"),
    HAR("prowl-export.har"),
}

object ProwlLogFormatter {
    fun export(logs: List<NetworkLog>, format: ProwlExportFormat): String =
        when (format) {
            ProwlExportFormat.FORMATTED_TEXT -> formattedText(logs)
            ProwlExportFormat.CURL_COMMANDS -> curlBundle(logs)
            ProwlExportFormat.HAR -> ProwlHarExporter.export(logs)
        }

    fun shareText(log: NetworkLog): String = formattedText(listOf(log))

    fun bodyText(body: NetworkLog.Body, pretty: Boolean = true): String =
        if (pretty) prettyBodyText(body) else BodyDecoder.toText(body.data, body.contentType)

    fun prettyBodyText(body: NetworkLog.Body): String {
        if (body.data.isEmpty()) return ""
        if (BodyDecoder.isBinaryContentType(body.contentType) &&
            !BodyDecoder.looksLikeText(body.data, body.contentType)
        ) {
            return BodyDecoder.hexPreview(body.data)
        }
        val raw = BodyDecoder.toText(body.data, body.contentType)
        if (raw.isBlank()) return BodyDecoder.hexPreview(body.data)
        return runCatching {
            val trimmed = raw.trim()
            when {
                trimmed.startsWith("{") || trimmed.startsWith("[") -> JsonPrettyPrinter.format(raw)
                else -> raw
            }
        }.getOrElse { raw }
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
                log.hostIp?.let { appendLine("Host IP: $it") }
                log.timing?.let { t ->
                    appendLine("DNS: ${t.dnsMillis ?: "-"}ms")
                    appendLine("Connect: ${t.connectMillis ?: "-"}ms")
                    appendLine("TLS: ${t.secureConnectMillis ?: "-"}ms")
                }
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
            val text = BodyDecoder.toText(body, log.requestBody?.contentType)
            builder.append("-d '${shellEscape(text)}' ")
        }
        builder.append("'${shellEscape(url)}'")
        return builder.toString().trim()
    }

    private fun shellEscape(value: String): String =
        value.replace("'", "'\\''")
}
