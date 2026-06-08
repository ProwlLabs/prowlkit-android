package com.prowllabs.prowl.core.mocking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class ProwlMockRule(
    val id: UUID = UUID.randomUUID(),
    val targetUrlPattern: String,
    val targetMethod: String = "ANY",
    val mockStatusCode: Int = 200,
    val mockBody: ByteArray = ByteArray(0),
    val mockHeaders: Map<String, String> = mapOf("Content-Type" to "application/json"),
    /** Artificial latency before the mocked response is returned. */
    val responseDelayMillis: Long = 0,
    val isEnabled: Boolean = true,
) {
    val mockBodyText: String
        get() = mockBody.toString(Charsets.UTF_8)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProwlMockRule) return false
        return id == other.id &&
            targetUrlPattern == other.targetUrlPattern &&
            targetMethod == other.targetMethod &&
            mockStatusCode == other.mockStatusCode &&
            mockBody.contentEquals(other.mockBody) &&
            mockHeaders == other.mockHeaders &&
            responseDelayMillis == other.responseDelayMillis &&
            isEnabled == other.isEnabled
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + targetUrlPattern.hashCode()
        result = 31 * result + targetMethod.hashCode()
        result = 31 * result + mockStatusCode
        result = 31 * result + mockBody.contentHashCode()
        result = 31 * result + mockHeaders.hashCode()
        result = 31 * result + responseDelayMillis.hashCode()
        result = 31 * result + isEnabled.hashCode()
        return result
    }
}

class ProwlMocker {
    private val lock = Any()
    private val rules = mutableListOf<ProwlMockRule>()
    private val _rulesFlow = MutableStateFlow<List<ProwlMockRule>>(emptyList())
    val rulesFlow: StateFlow<List<ProwlMockRule>> = _rulesFlow.asStateFlow()

    fun addRule(rule: ProwlMockRule) {
        saveRule(rule)
    }

    fun updateRule(rule: ProwlMockRule) {
        saveRule(rule)
    }

    /**
     * Saves a rule by id. When URL pattern + method match an existing rule, that rule is
     * overwritten instead of creating a duplicate.
     */
    fun saveRule(rule: ProwlMockRule) {
        synchronized(lock) {
            val byIdIndex = rules.indexOfFirst { it.id == rule.id }
            val byKeyIndex = rules.indexOfFirst { hasSameMatchKey(it, rule) && it.id != rule.id }

            when {
                byIdIndex >= 0 -> {
                    if (byKeyIndex >= 0) rules.removeAt(byKeyIndex)
                    rules[byIdIndex] = rule
                }
                byKeyIndex >= 0 -> {
                    rules[byKeyIndex] = rule.copy(id = rules[byKeyIndex].id)
                }
                else -> rules.add(rule)
            }
        }
        notifyChanged()
    }

    fun moveRuleUp(id: UUID) {
        synchronized(lock) {
            val index = rules.indexOfFirst { it.id == id }
            if (index > 0) {
                val rule = rules.removeAt(index)
                rules.add(index - 1, rule)
            }
        }
        notifyChanged()
    }

    fun moveRuleDown(id: UUID) {
        synchronized(lock) {
            val index = rules.indexOfFirst { it.id == id }
            if (index >= 0 && index < rules.lastIndex) {
                val rule = rules.removeAt(index)
                rules.add(index + 1, rule)
            }
        }
        notifyChanged()
    }

    fun rule(id: UUID): ProwlMockRule? = synchronized(lock) {
        rules.firstOrNull { it.id == id }
    }

    fun removeRule(id: UUID) {
        synchronized(lock) { rules.removeAll { it.id == id } }
        notifyChanged()
    }

    fun removeAllRules() {
        synchronized(lock) { rules.clear() }
        ProwlMockPersistence.clearAsync()
        publishRules()
    }

    fun replaceAllRules(newRules: List<ProwlMockRule>) {
        synchronized(lock) {
            rules.clear()
            rules.addAll(newRules)
        }
        publishRules()
    }

    fun allRules(): List<ProwlMockRule> = synchronized(lock) { rules.toList() }

    fun findMatch(url: String?, method: String): ProwlMockRule? {
        if (url.isNullOrBlank()) return null
        return synchronized(lock) {
            rules.firstOrNull { rule ->
                ProwlRuleMatcher.matches(
                    url = url,
                    method = method,
                    targetUrlPattern = rule.targetUrlPattern,
                    targetMethod = rule.targetMethod,
                    isEnabled = rule.isEnabled,
                )
            }
        }
    }

    private fun notifyChanged() {
        val snapshot = allRules()
        ProwlMockPersistence.persistAsync(snapshot)
        publishRules()
    }

    private fun publishRules() {
        _rulesFlow.value = allRules()
    }

    companion object {
        val shared = ProwlMocker()

        internal fun hasSameMatchKey(a: ProwlMockRule, b: ProwlMockRule): Boolean =
            a.targetUrlPattern.equals(b.targetUrlPattern, ignoreCase = true) &&
                a.targetMethod.equals(b.targetMethod, ignoreCase = true)
    }
}
