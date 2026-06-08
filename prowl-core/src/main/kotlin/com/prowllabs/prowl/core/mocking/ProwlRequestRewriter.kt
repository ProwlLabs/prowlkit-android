package com.prowllabs.prowl.core.mocking

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class ProwlRequestRewriter {
    private val lock = Any()
    private val rules = mutableListOf<ProwlRequestRewriteRule>()

    fun addRule(rule: ProwlRequestRewriteRule) {
        synchronized(lock) { rules.add(rule) }
        notifyChanged()
    }

    fun updateRule(rule: ProwlRequestRewriteRule) {
        synchronized(lock) {
            val index = rules.indexOfFirst { it.id == rule.id }
            if (index >= 0) rules[index] = rule
        }
        notifyChanged()
    }

    fun removeRule(id: UUID) {
        synchronized(lock) { rules.removeAll { it.id == id } }
        notifyChanged()
    }

    fun removeAllRules() {
        synchronized(lock) { rules.clear() }
        ProwlRequestRewritePersistence.clearAsync()
    }

    fun replaceAllRules(newRules: List<ProwlRequestRewriteRule>) {
        synchronized(lock) {
            rules.clear()
            rules.addAll(newRules)
        }
    }

    private fun notifyChanged() {
        ProwlRequestRewritePersistence.persistAsync(allRules())
    }

    fun allRules(): List<ProwlRequestRewriteRule> = synchronized(lock) { rules.toList() }

    fun findMatch(url: String?, method: String): ProwlRequestRewriteRule? {
        if (url.isNullOrBlank()) return null
        val normalizedMethod = method.uppercase()
        return synchronized(lock) {
            rules.firstOrNull { rule ->
                if (!rule.isEnabled || rule.targetUrlPattern.isEmpty()) return@firstOrNull false
                if (!url.contains(rule.targetUrlPattern, ignoreCase = true)) return@firstOrNull false
                if (rule.targetMethod.isNotEmpty() && rule.targetMethod.uppercase() != "ANY") {
                    if (normalizedMethod != rule.targetMethod.uppercase()) return@firstOrNull false
                }
                true
            }
        }
    }

    fun apply(request: Request, rule: ProwlRequestRewriteRule): Request {
        val builder = request.newBuilder()

        rule.replacementUrl.trim().takeIf { it.isNotEmpty() }?.let { replacement ->
            val newHttpUrl = if (replacement.contains("://")) {
                replacement.toHttpUrlOrNull()
            } else {
                val path = if (replacement.startsWith("/")) replacement else "/$replacement"
                val queryIndex = path.indexOf('?')
                request.url.newBuilder().apply {
                    if (queryIndex >= 0) {
                        encodedPath(path.substring(0, queryIndex))
                        encodedQuery(path.substring(queryIndex + 1))
                    } else {
                        encodedPath(path)
                    }
                }.build()
            }
            newHttpUrl?.let { builder.url(it) }
        }

        rule.headersToRemove.forEach { header ->
            builder.removeHeader(header)
        }
        rule.headerOverrides.forEach { (name, value) ->
            builder.header(name, value)
        }

        rule.replacementBody?.let { bodyBytes ->
            if (request.method !in METHODS_WITHOUT_BODY) {
                val mediaType = rule.replacementContentType?.toMediaTypeOrNull()
                    ?: request.body?.contentType()
                val body = bodyBytes.toRequestBody(mediaType)
                builder.method(request.method, body)
            }
        }

        return builder.build()
    }

    companion object {
        val shared = ProwlRequestRewriter()
        private val METHODS_WITHOUT_BODY = setOf("GET", "HEAD", "TRACE")
    }
}
