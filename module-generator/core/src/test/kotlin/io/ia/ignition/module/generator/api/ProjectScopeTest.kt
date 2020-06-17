package io.ia.ignition.module.generator.api

import io.ia.ignition.module.generator.api.ProjectScope.CLIENT
import io.ia.ignition.module.generator.api.ProjectScope.DESIGNER
import io.ia.ignition.module.generator.api.ProjectScope.GATEWAY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectScopeTest {

    @Test
    fun `valid scope shorthand string creates appropriate list of ProjectScopes`() {
        val cases = mapOf(
            "C" to listOf(CLIENT),
            "D" to listOf(DESIGNER),
            "G" to listOf(GATEWAY),
            "CD" to listOf(DESIGNER, CLIENT),
            "GCD" to listOf(GATEWAY, CLIENT, DESIGNER),
            "gcd" to listOf(GATEWAY, CLIENT, DESIGNER),
            "abc" to listOf(CLIENT),
            "xyz" to emptyList(),
            "GC" to listOf(GATEWAY, CLIENT),
            "GD" to listOf(GATEWAY, DESIGNER)
        )

        cases.keys.forEach {
            val scopes = ProjectScope.scopesFromShorthand(it)

            assertEquals(scopes.sorted(), cases[it]?.sorted())
        }
    }

    @Test
    fun `invalid scope shorthand string returns empty list`() {
        val cases = listOf("ZYX", "J", "12", "perry", "perspektive", "'", "")

        val emptyList = emptyList<ProjectScope>()

        cases.forEach {
            val scopes = ProjectScope.scopesFromShorthand(it)

            assertTrue(emptyList == scopes)
        }
    }
}
