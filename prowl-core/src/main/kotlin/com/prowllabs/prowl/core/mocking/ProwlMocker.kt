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
            isEnabled == other.isEnabled
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + targetUrlPattern.hashCode()
        result = 31 * result + targetMethod.hashCode()
        result = 31 * result + mockStatusCode
        result = 31 * result + mockBody.contentHashCode()
        result = 31 * result + mockHeaders.hashCode()
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
        synchronized(lock) { rules.add(rule) }
        notifyChanged()
    }

    fun updateRule(rule: ProwlMockRule) {
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
    }
}
