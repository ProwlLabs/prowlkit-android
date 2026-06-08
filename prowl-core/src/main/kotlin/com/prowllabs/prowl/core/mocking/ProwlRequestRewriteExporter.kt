package com.prowllabs.prowl.core.mocking

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ProwlRequestRewriteExporter {
    fun exportRules(rules: List<ProwlRequestRewriteRule>): String {
        val array = JSONArray()
        rules.forEach { rule -> array.put(ruleToJson(rule)) }
        return array.toString(2)
    }

    fun importRules(json: String): List<ProwlRequestRewriteRule> {
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

    private fun ruleToJson(rule: ProwlRequestRewriteRule): JSONObject = JSONObject().apply {
        put("id", rule.id.toString())
        put("targetUrlPattern", rule.targetUrlPattern)
        put("targetMethod", rule.targetMethod)
        put("replacementUrl", rule.replacementUrl)
        put("headerOverrides", JSONObject(rule.headerOverrides))
        put("headersToRemove", JSONArray(rule.headersToRemove.toList()))
        rule.replacementBody?.let {
            put("replacementBodyBase64", Base64.encodeToString(it, Base64.NO_WRAP))
        }
        rule.replacementContentType?.let { put("replacementContentType", it) }
        put("isEnabled", rule.isEnabled)
    }

    private fun jsonToRule(obj: JSONObject): ProwlRequestRewriteRule {
        val headersJson = obj.optJSONObject("headerOverrides")
        val headerOverrides = buildMap {
            headersJson?.keys()?.forEach { key ->
                put(key, headersJson.getString(key))
            }
        }
        val removeArray = obj.optJSONArray("headersToRemove")
        val headersToRemove = buildSet {
            if (removeArray != null) {
                for (i in 0 until removeArray.length()) {
                    add(removeArray.getString(i))
                }
            }
        }
        val bodyBase64 = obj.optString("replacementBodyBase64", "")
        val replacementBody = when {
            obj.has("replacementBodyBase64") && bodyBase64.isNotBlank() ->
                Base64.decode(bodyBase64, Base64.DEFAULT)
            obj.has("replacementBody") ->
                obj.optString("replacementBody", "").toByteArray(Charsets.UTF_8)
            else -> null
        }
        return ProwlRequestRewriteRule(
            id = obj.optString("id").let { if (it.isBlank()) UUID.randomUUID() else UUID.fromString(it) },
            targetUrlPattern = obj.getString("targetUrlPattern"),
            targetMethod = obj.optString("targetMethod", "ANY"),
            replacementUrl = obj.optString("replacementUrl", ""),
            headerOverrides = headerOverrides,
            headersToRemove = headersToRemove,
            replacementBody = replacementBody,
            replacementContentType = obj.optString("replacementContentType").takeIf { it.isNotBlank() },
            isEnabled = obj.optBoolean("isEnabled", true),
        )
    }
}
