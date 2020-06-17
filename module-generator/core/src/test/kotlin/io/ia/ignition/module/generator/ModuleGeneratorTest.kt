package io.ia.ignition.module.generator

import io.ia.ignition.module.generator.api.GeneratorConfig
import io.ia.ignition.module.generator.api.TemplateMarker
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModuleGeneratorTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    data class TestConfig(val moduleName: String, val packageName: String, val scope: String, val dir: Path)

    private fun dir(folderName: String): Path {
        return tempFolder.newFolder(folderName).toPath()
    }

    @Test
    fun `module generator runs with valid config without error`() {
        listOf(
            TestConfig("The Greatness", "le.examp", "G", dir("v1")),
            TestConfig("almost Greatness", "le.examp.odd", "GC", dir("v2")),
            TestConfig("The greatness", "le.examp.whoa", "GCD", dir("v3")),
            TestConfig("oncegreatness", "buenos.dias.amigo", "GCD", dir("v4")),
            TestConfig("The Greatness", "le.pant", "CD", dir("v5")),
            TestConfig("A Goodness", "come.va", "C", dir("v6")),
            TestConfig("The number 1 Greatness", "bon.gior.nio", "D", dir("v7"))
        ).forEach {
            val config = GeneratorConfig.ConfigBuilder()
                .moduleName(it.moduleName)
                .packageName(it.packageName)
                .parentDir(it.dir)
                .scopes(it.scope)
                .build()

            var t: Throwable? = null
            var projectRootDir: Path? = null

            try {
                projectRootDir = ModuleGenerator.generate(config)
            } catch (e: Exception) {
                t = e
            }

            assertNull(t)
            assertTrue(
                null != projectRootDir &&
                    Files.exists(projectRootDir) &&
                    Files.isDirectory(projectRootDir),
                "projectRootDir exists as directory"
            )
        }
    }

    @Test
    fun `module generator runs with appropriate validation errors`() {
        val parentDir = tempFolder.newFolder().toPath()
        Files.createDirectories(parentDir)

        val badScopes = "AAABBB"
        val badPackageName = "le.examp....@"
        val badModuleName = "My Test Module"

        val config = GeneratorConfig.ConfigBuilder()
            .moduleName(badModuleName)
            .packageName(badPackageName)
            .parentDir(parentDir)
            .scopes(badScopes)
            .build()

        var t: Throwable? = null

        try {
            ModuleGenerator.generate(config)
        } catch (e: Exception) {
            t = e
        }

        assertNotNull(t)
        val errMsg = t.message
        assertNotNull(errMsg)
        assertTrue(errMsg.contains("The module name $badModuleName ends with the suffix \"Module\"."))
        assertTrue(errMsg.contains("The package path $badPackageName is not a valid path."))
        assertTrue(errMsg.contains("No valid scopes were found in $badScopes.)"))
    }

    @Test
    fun `created settings file has correct project name`() {
        val parentDir = tempFolder.newFolder().toPath()

        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir)
        } else {
            parentDir.toFile().deleteRecursively()
            Files.createDirectories(parentDir)
        }
        val scopes = "GCD"
        val pkg = "bot.skynet.terminator"
        val name = "T Two Hundred"

        val config = GeneratorConfig.ConfigBuilder()
            .moduleName(name)
            .packageName(pkg)
            .parentDir(parentDir)
            .scopes(scopes)
            .build()

        var t: Throwable? = null
        var projDir: Path? = null

        try {
            projDir = ModuleGenerator.generate(config)
        } catch (e: Exception) {
            t = e
        }

        assertNull(t)
        assertNotNull(projDir)

        val settingsFile = projDir.resolve("settings.gradle")
        val settingsText = settingsFile.toFile().readText(Charsets.UTF_8)
        assertTrue(Files.exists(settingsFile))
        assertFalse(settingsText.contains(TemplateMarker.ROOT_PROJECT_NAME.key, false))

        val projDirName = projDir.fileName

        assertTrue(settingsText.contains(projDirName.toString()))
    }
}
