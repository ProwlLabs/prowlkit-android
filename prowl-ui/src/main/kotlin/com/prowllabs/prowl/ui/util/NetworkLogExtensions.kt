package com.prowllabs.prowl.ui.util

import com.prowllabs.prowl.core.model.NetworkLog
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val detailDateFormat = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.getDefault())
}

fun NetworkLog.urlPath(): String = runCatching {
    val path = URI(url ?: return "/").path
    path.ifEmpty { "/" }
}.getOrElse { "/" }

fun NetworkLog.endpointKey(): String = "${method.uppercase()}::${urlPath()}"

fun NetworkLog.urlPathWithQuery(): String = runCatching {
    val uri = URI(url ?: return "/")
    val path = uri.path.ifEmpty { "/" }
    val query = uri.query
    if (query.isNullOrBlank()) path else "$path?$query"
}.getOrElse { "/" }

fun NetworkLog.urlHost(): String? = runCatching {
    URI(url ?: return null).host
}.getOrNull()

fun NetworkLog.urlQueryItems(): List<Pair<String, String>> = runCatching {
    val query = URI(url ?: return emptyList()).query ?: return emptyList()
    query.split("&").mapNotNull { part ->
        val idx = part.indexOf('=')
        if (idx <= 0) {
            part to ""
        } else {
            part.substring(0, idx) to part.substring(idx + 1)
        }
    }
}.getOrElse { emptyList() }

fun NetworkLog.formattedStartedAt(): String =
    detailDateFormat.get().format(Date(startedAtMillis))

fun NetworkLog.formattedResponseAt(): String =
    detailDateFormat.get().format(Date(startedAtMillis + durationMillis))

fun NetworkLog.formattedDurationSeconds(): String =
    String.format(Locale.US, "%.6f", durationMillis / 1000.0)

fun NetworkLog.formattedDuration(): String =
    String.format(Locale.US, "%.3fs", durationMillis / 1000.0)
