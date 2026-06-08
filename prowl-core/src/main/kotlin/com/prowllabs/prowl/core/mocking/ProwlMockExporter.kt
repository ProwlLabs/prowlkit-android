package com.prowllabs.prowl.core.mocking

import org.json.JSONArray
import org.json.JSONObject
import android.util.Base64
import java.util.UUID

object ProwlMockExporter {
    fun exportRules(rules: List<ProwlMockRule>): String {
        val array = JSONArray()
        rules.forEach { rule -> array.put(ruleToJson(rule)) }
        return array.toString(2)
    }

    fun importRules(json: String): List<ProwlMockRule> {
        val trimmed = json.trim()
        return when {
            trimmed.startsWith("[") -> {
                val array = JSONArray(trimmed)
                (0 until array.length()).mapNotNull { index ->
                    runCatching { jsonToRule(array.getJSONObject(index)) }.getOrNull()
                }
            }
            trimmed.startsWith("{") -> listOf(jsonToRule(JSONObject(trimmed)))
            else -> emptyList()
        }
    }

    private fun ruleToJson(rule: ProwlMockRule): JSONObject = JSONObject().apply {
        put("id", rule.id.toString())
        put("targetUrlPattern", rule.targetUrlPattern)
        put("targetMethod", rule.targetMethod)
        put("mockStatusCode", rule.mockStatusCode)
        put("mockBodyBase64", Base64.encodeToString(rule.mockBody, Base64.NO_WRAP))
        put("mockHeaders", JSONObject(rule.mockHeaders))
        put("responseDelayMillis", rule.responseDelayMillis)
        put("isEnabled", rule.isEnabled)
    }

    private fun jsonToRule(obj: JSONObject): ProwlMockRule {
        val headersJson = obj.optJSONObject("mockHeaders")
        val headers = buildMap {
            headersJson?.keys()?.forEach { key ->
                put(key, headersJson.getString(key))
            }
        }
        val bodyBase64 = obj.optString("mockBodyBase64", "")
        val body = if (bodyBase64.isNotBlank()) {
            Base64.decode(bodyBase64, Base64.DEFAULT)
        } else {
            obj.optString("mockBody", "").toByteArray(Charsets.UTF_8)
        }
        return ProwlMockRule(
            id = obj.optString("id").let { if (it.isBlank()) UUID.randomUUID() else UUID.fromString(it) },
            targetUrlPattern = obj.getString("targetUrlPattern"),
            targetMethod = obj.optString("targetMethod", "ANY"),
            mockStatusCode = obj.optInt("mockStatusCode", 200),
            mockBody = body,
            mockHeaders = headers.ifEmpty { mapOf("Content-Type" to "application/json") },
            responseDelayMillis = obj.optLong("responseDelayMillis", 0).coerceAtLeast(0),
            isEnabled = obj.optBoolean("isEnabled", true),
        )
    }
}
