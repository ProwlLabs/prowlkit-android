package com.prowllabs.prowl.core.storage

import android.util.Base64
import com.prowllabs.prowl.core.model.MultipartPart
import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.model.NetworkProtocol
import com.prowllabs.prowl.core.model.RequestTiming
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal object NetworkLogSerializer {
    fun toJson(logs: List<NetworkLog>): String {
        val array = JSONArray()
        logs.forEach { array.put(toJsonObject(it)) }
        return array.toString()
    }

    fun fromJson(json: String): List<NetworkLog> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    runCatching { fromJsonObject(array.getJSONObject(i)) }.getOrNull()?.let(::add)
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun toJsonObject(log: NetworkLog): JSONObject = JSONObject().apply {
        put("id", log.id.toString())
        put("requestId", log.requestId.toString())
        put("url", log.url)
        put("method", log.method)
        put("requestHeaders", JSONObject(log.requestHeaders))
        put("responseHeaders", JSONObject(log.responseHeaders))
        log.requestBody?.let { put("requestBody", bodyToJson(it)) }
        log.responseBody?.let { put("responseBody", bodyToJson(it)) }
        log.statusCode?.let { put("statusCode", it) }
        put("startedAtMillis", log.startedAtMillis)
        put("durationMillis", log.durationMillis)
        log.timeoutMillis?.let { put("timeoutMillis", it) }
        log.cachePolicy?.let { put("cachePolicy", it) }
        log.errorDescription?.let { put("errorDescription", it) }
        put("endpointRateAlertTriggered", log.endpointRateAlertTriggered)
        log.hostIp?.let { put("hostIp", it) }
        put("protocol", log.protocol.name)
        log.timing?.let { put("timing", timingToJson(it)) }
        if (log.requestMultipartParts.isNotEmpty()) {
            put("requestMultipartParts", multipartToJson(log.requestMultipartParts))
        }
        if (log.responseMultipartParts.isNotEmpty()) {
            put("responseMultipartParts", multipartToJson(log.responseMultipartParts))
        }
        put("requestRewritten", log.requestRewritten)
    }

    private fun fromJsonObject(obj: JSONObject): NetworkLog = NetworkLog(
        id = UUID.fromString(obj.getString("id")),
        requestId = UUID.fromString(obj.getString("requestId")),
        url = obj.optString("url").takeIf { it.isNotBlank() },
        method = obj.getString("method"),
        requestHeaders = jsonToMap(obj.optJSONObject("requestHeaders")),
        requestBody = obj.optJSONObject("requestBody")?.let(::bodyFromJson),
        responseHeaders = jsonToMap(obj.optJSONObject("responseHeaders")),
        responseBody = obj.optJSONObject("responseBody")?.let(::bodyFromJson),
        statusCode = obj.optInt("statusCode").takeIf { obj.has("statusCode") && !obj.isNull("statusCode") },
        startedAtMillis = obj.getLong("startedAtMillis"),
        durationMillis = obj.getLong("durationMillis"),
        timeoutMillis = obj.optLong("timeoutMillis").takeIf { obj.has("timeoutMillis") },
        cachePolicy = obj.optString("cachePolicy").takeIf { it.isNotBlank() },
        errorDescription = obj.optString("errorDescription").takeIf { it.isNotBlank() },
        endpointRateAlertTriggered = obj.optBoolean("endpointRateAlertTriggered"),
        hostIp = obj.optString("hostIp").takeIf { it.isNotBlank() },
        protocol = runCatching { NetworkProtocol.valueOf(obj.optString("protocol", "HTTP")) }
            .getOrDefault(NetworkProtocol.HTTP),
        timing = obj.optJSONObject("timing")?.let(::timingFromJson),
        requestMultipartParts = obj.optJSONArray("requestMultipartParts")?.let(::multipartFromJson).orEmpty(),
        responseMultipartParts = obj.optJSONArray("responseMultipartParts")?.let(::multipartFromJson).orEmpty(),
        requestRewritten = obj.optBoolean("requestRewritten"),
    )

    private fun bodyToJson(body: NetworkLog.Body): JSONObject = JSONObject().apply {
        put("data", Base64.encodeToString(body.data, Base64.NO_WRAP))
        body.contentType?.let { put("contentType", it) }
    }

    private fun bodyFromJson(obj: JSONObject): NetworkLog.Body = NetworkLog.Body(
        data = Base64.decode(obj.getString("data"), Base64.NO_WRAP),
        contentType = obj.optString("contentType").takeIf { it.isNotBlank() },
    )

    private fun timingToJson(t: RequestTiming): JSONObject = JSONObject().apply {
        t.dnsMillis?.let { put("dnsMillis", it) }
        t.connectMillis?.let { put("connectMillis", it) }
        t.secureConnectMillis?.let { put("secureConnectMillis", it) }
        t.requestHeadersMillis?.let { put("requestHeadersMillis", it) }
        t.requestBodyMillis?.let { put("requestBodyMillis", it) }
        t.responseHeadersMillis?.let { put("responseHeadersMillis", it) }
        t.responseBodyMillis?.let { put("responseBodyMillis", it) }
    }

    private fun timingFromJson(obj: JSONObject): RequestTiming = RequestTiming(
        dnsMillis = obj.optLong("dnsMillis").takeIf { obj.has("dnsMillis") },
        connectMillis = obj.optLong("connectMillis").takeIf { obj.has("connectMillis") },
        secureConnectMillis = obj.optLong("secureConnectMillis").takeIf { obj.has("secureConnectMillis") },
        requestHeadersMillis = obj.optLong("requestHeadersMillis").takeIf { obj.has("requestHeadersMillis") },
        requestBodyMillis = obj.optLong("requestBodyMillis").takeIf { obj.has("requestBodyMillis") },
        responseHeadersMillis = obj.optLong("responseHeadersMillis").takeIf { obj.has("responseHeadersMillis") },
        responseBodyMillis = obj.optLong("responseBodyMillis").takeIf { obj.has("responseBodyMillis") },
    )

    private fun multipartToJson(parts: List<MultipartPart>): JSONArray {
        val array = JSONArray()
        parts.forEach { part ->
            array.put(
                JSONObject().apply {
                    part.name?.let { put("name", it) }
                    part.fileName?.let { put("fileName", it) }
                    part.contentType?.let { put("contentType", it) }
                    put("headers", JSONObject(part.headers))
                    put("sizeBytes", part.sizeBytes)
                    part.textPreview?.let { put("textPreview", it) }
                    put("isBinary", part.isBinary)
                },
            )
        }
        return array
    }

    private fun multipartFromJson(array: JSONArray): List<MultipartPart> = buildList {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            add(
                MultipartPart(
                    name = obj.optString("name").takeIf { it.isNotBlank() },
                    fileName = obj.optString("fileName").takeIf { it.isNotBlank() },
                    contentType = obj.optString("contentType").takeIf { it.isNotBlank() },
                    headers = jsonToMap(obj.optJSONObject("headers")),
                    sizeBytes = obj.optInt("sizeBytes"),
                    textPreview = obj.optString("textPreview").takeIf { it.isNotBlank() },
                    isBinary = obj.optBoolean("isBinary"),
                ),
            )
        }
    }

    private fun jsonToMap(obj: JSONObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        return buildMap {
            obj.keys().forEach { key -> put(key, obj.getString(key)) }
        }
    }
}
