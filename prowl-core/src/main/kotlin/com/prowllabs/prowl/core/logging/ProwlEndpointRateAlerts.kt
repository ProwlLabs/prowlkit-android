package com.prowllabs.prowl.core.logging

import com.prowllabs.prowl.core.model.NetworkLog
import java.util.UUID

/** A rule that flags an endpoint when request volume crosses a threshold. */
data class ProwlEndpointRateAlertRule(
    val id: UUID = UUID.randomUUID(),
    val match: Match,
    val threshold: Int,
) {
    sealed interface Match {
        data class UrlContains(val fragment: String) : Match
        data class UrlRegularExpression(val pattern: String) : Match
    }
}

object ProwlEndpointRateAlerts {
    private val coordinator = ProwlEndpointRateAlertCoordinator()

    var rules: List<ProwlEndpointRateAlertRule>
        get() = coordinator.rules
        set(value) { coordinator.rules = value }

    fun resetCounters() = coordinator.reset()

    internal fun evaluate(log: NetworkLog): Boolean = coordinator.evaluate(log)
}

private class ProwlEndpointRateAlertCoordinator {
    private val lock = Any()
    private val rulesStorage = linkedMapOf<UUID, ProwlEndpointRateAlertRule>()
    private val counts = mutableMapOf<String, Int>()

    var rules: List<ProwlEndpointRateAlertRule>
        get() = synchronized(lock) { rulesStorage.values.toList() }
        set(value) = synchronized(lock) {
            rulesStorage.clear()
            value.forEach { rulesStorage[it.id] = it }
        }

    fun reset() = synchronized(lock) { counts.clear() }

    fun evaluate(log: NetworkLog): Boolean {
        val url = log.url ?: return false
        val keyBase = "${log.method.uppercase()}|${url.substringBefore('?')}"

        return synchronized(lock) {
            rulesStorage.values.any { rule ->
                if (!matches(rule, url)) return@any false
                val counterKey = "${rule.id}|$keyBase"
                val next = (counts[counterKey] ?: 0) + 1
                counts[counterKey] = next
                next == rule.threshold.coerceAtLeast(1)
            }
        }
    }

    private fun matches(rule: ProwlEndpointRateAlertRule, url: String): Boolean =
        when (val match = rule.match) {
            is ProwlEndpointRateAlertRule.Match.UrlContains ->
                url.contains(match.fragment, ignoreCase = true)
            is ProwlEndpointRateAlertRule.Match.UrlRegularExpression ->
                runCatching { Regex(match.pattern).containsMatchIn(url) }.getOrDefault(false)
        }
}

/** Decode response bodies for display only — live responses are unchanged. */
fun interface ResponseBodyLoggingTransformer {
    fun transform(body: ByteArray, contentType: String?): ByteArray?
}
