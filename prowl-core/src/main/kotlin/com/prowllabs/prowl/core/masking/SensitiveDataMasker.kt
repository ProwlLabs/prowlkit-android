package com.prowllabs.prowl.core.masking

import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.util.BodyDecoder
import com.prowllabs.prowl.core.util.JsonPrettyPrinter

class SensitiveDataMasker(
    val sensitiveHeaders: Set<String> = DEFAULT_SENSITIVE_HEADERS,
    val sensitiveJsonKeys: Set<String> = DEFAULT_SENSITIVE_JSON_KEYS,
    val redactionToken: String = "[REDACTED]",
) {
    private val normalizedHeaders = sensitiveHeaders.map { it.lowercase() }.toSet()
    private val normalizedJsonKeys = sensitiveJsonKeys.map { it.lowercase() }.toSet()

    fun mask(headers: Map<String, String>): Map<String, String> =
        headers.mapValues { (key, value) ->
            if (key.lowercase() in normalizedHeaders) redactionToken else value
        }

    fun mask(body: ByteArray?, contentType: String?): NetworkLog.Body? {
        if (body == null) return null
        val normalizedContentType = contentType?.lowercase().orEmpty()
        val maskedData = when {
            normalizedContentType.contains("json") || looksLikeJson(body, contentType) ->
                maskJsonBody(body, contentType) ?: maskTextBody(body, contentType)
            else -> maskTextBody(body, contentType)
        }
        return NetworkLog.Body(maskedData, contentType)
    }

    private fun maskJsonBody(body: ByteArray, contentType: String?): ByteArray? {
        val charset = BodyDecoder.charsetFromContentType(contentType)
        return runCatching {
            val text = BodyDecoder.toText(body, contentType)
            val json = org.json.JSONObject(text)
            maskJsonObject(json)
            JsonPrettyPrinter.format(json).toByteArray(charset)
        }.getOrNull() ?: runCatching {
            val text = BodyDecoder.toText(body, contentType)
            val json = org.json.JSONArray(text)
            maskJsonArray(json)
            JsonPrettyPrinter.format(json).toByteArray(charset)
        }.getOrNull()
    }

    private fun maskJsonObject(json: org.json.JSONObject) {
        val keys = json.keys().asSequence().toList()
        for (key in keys) {
            when (val value = json.get(key)) {
                is org.json.JSONObject -> maskJsonObject(value)
                is org.json.JSONArray -> maskJsonArray(value)
                else -> if (key.lowercase() in normalizedJsonKeys) {
                    json.put(key, redactionToken)
                }
            }
        }
    }

    private fun maskJsonArray(array: org.json.JSONArray) {
        for (index in 0 until array.length()) {
            when (val value = array.get(index)) {
                is org.json.JSONObject -> maskJsonObject(value)
                is org.json.JSONArray -> maskJsonArray(value)
            }
        }
    }

    private fun maskTextBody(body: ByteArray, contentType: String?): ByteArray {
        val charset = BodyDecoder.charsetFromContentType(contentType)
        var text = BodyDecoder.toText(body, contentType)
        BEARER_REGEX.replace(text) { "${it.groupValues[1]}$redactionToken" }.also { text = it }
        COOKIE_REGEX.replace(text) { "${it.groupValues[1]}$redactionToken" }.also { text = it }
        PEM_REGEX.replace(text, redactionToken).also { text = it }
        return text.toByteArray(charset)
    }

    private fun looksLikeJson(body: ByteArray, contentType: String?): Boolean {
        val trimmed = BodyDecoder.toText(body, contentType).trimStart()
        return trimmed.startsWith("{") || trimmed.startsWith("[")
    }

    companion object {
        val DEFAULT_SENSITIVE_HEADERS = setOf(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "x-auth-token",
        )

        val DEFAULT_SENSITIVE_JSON_KEYS = setOf(
            "password",
            "passcode",
            "token",
            "access_token",
            "refresh_token",
            "id_token",
            "bearer",
            "authorization",
            "cookie",
            "private_key",
            "privatekey",
            "client_secret",
            "secret",
        )

        private val BEARER_REGEX = Regex("(?i)(Bearer\\s+)[^\\s\"']+")
        private val COOKIE_REGEX = Regex("(?i)(Cookie:\\s*)[^\\n\\r]+")
        private val PEM_REGEX = Regex(
            "-----BEGIN [A-Z ]+PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]+PRIVATE KEY-----",
            RegexOption.MULTILINE,
        )
    }
}
