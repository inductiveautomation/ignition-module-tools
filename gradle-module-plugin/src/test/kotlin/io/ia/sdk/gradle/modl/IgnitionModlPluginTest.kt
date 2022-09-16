package io.ia.sdk.gradle.modl

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import kotlin.test.assertNotNull

class IgnitionModlPluginTest {
    @Test
    fun `plugin applies without error task`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.ia.sdk.modl")

        // Verify the result
        assertNotNull(project.tasks.findByName("assemble"))
    }
}
