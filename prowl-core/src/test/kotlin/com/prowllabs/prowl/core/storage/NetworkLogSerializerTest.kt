package com.prowllabs.prowl.core.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkLogSerializerTest {
    @Test
    fun fromJson_blank_returnsEmptyList() {
        assertEquals(0, NetworkLogSerializer.fromJson("").size)
    }

    @Test
    fun fromJson_invalid_returnsEmptyList() {
        assertEquals(0, NetworkLogSerializer.fromJson("not-json").size)
    }
}
