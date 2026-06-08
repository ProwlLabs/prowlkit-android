package com.prowllabs.prowl.core.mocking

internal object ProwlRuleMatcher {
    fun matches(
        url: String?,
        method: String,
        targetUrlPattern: String,
        targetMethod: String,
        isEnabled: Boolean,
    ): Boolean {
        if (!isEnabled || targetUrlPattern.isEmpty()) return false
        val absoluteUrl = url ?: return false
        if (!absoluteUrl.contains(targetUrlPattern, ignoreCase = true)) return false
        if (targetMethod.isNotEmpty() && targetMethod.uppercase() != "ANY") {
            if (method.uppercase() != targetMethod.uppercase()) return false
        }
        return true
    }
}
