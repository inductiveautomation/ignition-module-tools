package io.ia.ignition.module.generator.data

import io.ia.ignition.module.generator.api.GeneratorConfigBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertTrue

class GeneratorConfigTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    companion object {
        val scopes = "AAAABBBBB"
        val packageName = "le.examp"
        val moduleName = "My Test Module"
    }

    @Test
    fun `builder generates valid config object`() {

        val parentDir = tempFolder.newFolder()

        val config1 = GeneratorConfigBuilder()
            .moduleName(moduleName)
            .packageName(packageName)
            .parentDir(parentDir.toPath())
            .scopes(scopes)
            .build()

        val config2 = GeneratorConfigBuilder()
            .moduleName(moduleName)
            .packageName(packageName)
            .parentDir(parentDir.toPath())
            .scopes(scopes)
            .build()

        assertTrue((config1 == config2), "config objects built with same values should pass equality")
    }
}
