package com.prowllabs.prowl.core

/**
 * Marks library-internal API. Prefer [com.prowllabs.prowl.Prowl] in application code.
 */
@RequiresOptIn(
    message = "ProwlRuntime and related types are for ProwlKit modules only. Use the Prowl facade.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
annotation class ProwlInternalApi
