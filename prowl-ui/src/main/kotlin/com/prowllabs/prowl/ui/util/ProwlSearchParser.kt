package com.prowllabs.prowl.ui.util

import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.util.BodyDecoder

data class ProwlSearchQuery(
    val freeText: String = "",
    val method: String? = null,
    val status: Int? = null,
    val statusRange: IntRange? = null,
    val host: String? = null,
)

object ProwlSearchParser {
    private val tokenPattern = Regex("""(\w+):(\S+)""")

    fun parse(input: String): ProwlSearchQuery {
        if (input.isBlank()) return ProwlSearchQuery()
        var method: String? = null
        var status: Int? = null
        var statusRange: IntRange? = null
        var host: String? = null
        val freeParts = mutableListOf<String>()

        input.split("\\s+".toRegex()).forEach { token ->
            when {
                token.startsWith("method:", ignoreCase = true) -> {
                    method = token.substringAfter(":").uppercase()
                }
                token.startsWith("status:", ignoreCase = true) -> {
                    val value = token.substringAfter(":")
                    when {
                        value.endsWith("xx", ignoreCase = true) && value.length == 3 -> {
                            val prefix = value.first().digitToIntOrNull() ?: return@forEach
                            statusRange = (prefix * 100)..(prefix * 100 + 99)
                        }
                        else -> status = value.toIntOrNull()
                    }
                }
                token.startsWith("host:", ignoreCase = true) -> {
                    host = token.substringAfter(":")
                }
                else -> freeParts.add(token)
            }
        }

        return ProwlSearchQuery(
            freeText = freeParts.joinToString(" "),
            method = method,
            status = status,
            statusRange = statusRange,
            host = host,
        )
    }

    fun matches(log: NetworkLog, query: ProwlSearchQuery): Boolean {
        query.method?.let { if (!log.method.equals(it, ignoreCase = true)) return false }
        query.status?.let { if (log.statusCode != it) return false }
        query.statusRange?.let { range ->
            val code = log.statusCode ?: return false
            if (code !in range) return false
        }
        query.host?.let { h ->
            val logHost = runCatching {
                java.net.URI(log.url ?: return false).host
            }.getOrNull() ?: return false
            if (!logHost.contains(h, ignoreCase = true)) return false
        }
        if (query.freeText.isBlank()) return true
        return matchesFreeText(log, query.freeText)
    }

    private fun matchesFreeText(log: NetworkLog, q: String): Boolean {
        val lower = q.lowercase()
        if (log.url?.lowercase()?.contains(lower) == true) return true
        if (log.method.lowercase().contains(lower)) return true
        if (log.statusCode?.toString()?.contains(lower) == true) return true
        if (log.requestHeaders.values.any { it.lowercase().contains(lower) }) return true
        if (log.responseHeaders.values.any { it.lowercase().contains(lower) }) return true
        log.responseBody?.let { BodyDecoder.toText(it.data, it.contentType).lowercase() }
            ?.let { if (it.contains(lower)) return true }
        log.requestBody?.let { BodyDecoder.toText(it.data, it.contentType).lowercase() }
            ?.let { if (it.contains(lower)) return true }
        return false
    }
}
