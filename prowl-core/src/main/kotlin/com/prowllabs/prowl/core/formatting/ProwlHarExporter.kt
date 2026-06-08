package com.prowllabs.prowl.core.formatting

import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.util.BodyDecoder
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ProwlHarExporter {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun export(logs: List<NetworkLog>): String {
        val entries = JSONArray()
        logs.forEach { log -> entries.put(entry(log)) }
        return JSONObject()
            .put("log", JSONObject()
                .put("version", "1.2")
                .put("creator", JSONObject()
                    .put("name", "Prowl")
                    .put("version", "0.2.0"))
                .put("entries", entries))
            .toString(2)
    }

    private fun entry(log: NetworkLog): JSONObject = JSONObject().apply {
        put("startedDateTime", isoFormat.format(Date(log.startedAtMillis)))
        put("time", log.durationMillis.toDouble())
        put("request", request(log))
        put("response", response(log))
        put("cache", JSONObject())
        put("timings", timings(log))
        log.hostIp?.let { put("serverIPAddress", it) }
        log.url?.let { put("comment", it) }
    }

    private fun request(log: NetworkLog): JSONObject = JSONObject().apply {
        put("method", log.method)
        put("url", log.url ?: "")
        put("httpVersion", "HTTP/1.1")
        put("headers", headersArray(log.requestHeaders))
        put("queryString", JSONArray())
        put("headersSize", -1)
        put("bodySize", log.requestBody?.data?.size ?: 0)
        log.requestBody?.let { body ->
            put("postData", JSONObject()
                .put("mimeType", body.contentType ?: "application/octet-stream")
                .put("text", BodyDecoder.toText(body.data, body.contentType)))
        }
    }

    private fun response(log: NetworkLog): JSONObject = JSONObject().apply {
        put("status", log.statusCode ?: 0)
        put("statusText", log.errorDescription ?: "")
        put("httpVersion", "HTTP/1.1")
        put("headers", headersArray(log.responseHeaders))
        put("content", JSONObject().apply {
            log.responseBody?.let { body ->
                put("mimeType", body.contentType ?: "application/octet-stream")
                put("size", body.data.size)
                put("text", BodyDecoder.toText(body.data, body.contentType))
            }
        })
        put("headersSize", -1)
        put("bodySize", log.responseBody?.data?.size ?: 0)
    }

    private fun headersArray(headers: Map<String, String>): JSONArray {
        val array = JSONArray()
        headers.forEach { (name, value) ->
            array.put(JSONObject().put("name", name).put("value", value))
        }
        return array
    }

    private fun timings(log: NetworkLog): JSONObject {
        val t = log.timing
        return JSONObject().apply {
            put("send", (t?.requestBodyMillis ?: 0).toDouble())
            put("wait", (t?.responseHeadersMillis ?: log.durationMillis).toDouble())
            put("receive", (t?.responseBodyMillis ?: 0).toDouble())
            put("dns", (t?.dnsMillis ?: -1).toDouble())
            put("connect", (t?.connectMillis ?: -1).toDouble())
            put("ssl", (t?.secureConnectMillis ?: -1).toDouble())
        }
    }
}
