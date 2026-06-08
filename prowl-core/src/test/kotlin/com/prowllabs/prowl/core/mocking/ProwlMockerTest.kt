package com.prowllabs.prowl.core.mocking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ProwlMockerTest {
    private lateinit var mocker: ProwlMocker

    @Before
    fun setUp() {
        mocker = ProwlMocker()
    }

    @Test
    fun saveRule_samePatternAndMethod_overwritesInsteadOfDuplicating() {
        val firstId = UUID.randomUUID()
        mocker.saveRule(
            ProwlMockRule(
                id = firstId,
                targetUrlPattern = "/users",
                targetMethod = "GET",
                mockStatusCode = 200,
                mockBody = """{"v":1}""".toByteArray(),
            ),
        )
        mocker.saveRule(
            ProwlMockRule(
                targetUrlPattern = "/users",
                targetMethod = "GET",
                mockStatusCode = 418,
                mockBody = """{"v":2}""".toByteArray(),
            ),
        )

        assertEquals(1, mocker.allRules().size)
        val rule = mocker.allRules().single()
        assertEquals(firstId, rule.id)
        assertEquals(418, rule.mockStatusCode)
    }

    @Test
    fun findMatch_usesFirstRuleInListOrder() {
        mocker.saveRule(
            ProwlMockRule(
                targetUrlPattern = "/api",
                targetMethod = "ANY",
                mockStatusCode = 201,
            ),
        )
        mocker.saveRule(
            ProwlMockRule(
                targetUrlPattern = "/api/users",
                targetMethod = "ANY",
                mockStatusCode = 202,
            ),
        )

        val match = mocker.findMatch("https://example.com/api/users", "GET")
        assertEquals(201, match?.mockStatusCode)
    }

    @Test
    fun moveRuleUp_changesPriority() {
        val first = ProwlMockRule(targetUrlPattern = "/a", mockStatusCode = 1)
        val second = ProwlMockRule(targetUrlPattern = "/b", mockStatusCode = 2)
        mocker.saveRule(first)
        mocker.saveRule(second)

        mocker.moveRuleUp(second.id)

        assertEquals(2, mocker.allRules().first().mockStatusCode)
    }

    @Test
    fun findMatch_disabledRule_returnsNull() {
        mocker.saveRule(
            ProwlMockRule(
                targetUrlPattern = "/users",
                isEnabled = false,
            ),
        )
        assertNull(mocker.findMatch("https://example.com/users", "GET"))
    }
}
