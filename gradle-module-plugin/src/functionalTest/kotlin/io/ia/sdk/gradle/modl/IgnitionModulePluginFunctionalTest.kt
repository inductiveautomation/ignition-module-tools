
package io.ia.sdk.gradle.modl

import io.ia.ignition.module.generator.ModuleGenerator
import io.ia.ignition.module.generator.api.GeneratorConfig
import io.ia.sdk.gradle.modl.util.nameToDirName
import io.ia.sdk.gradle.modl.util.signedModuleName
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

/**
 * Functional tests for the 'io.ia.gradle.module.greeting' plugin.
 */
class IgnitionModulePluginFunctionalTest : BaseTest() {

    @Test
    fun `plugin gets applied and executes task help with minimal configuration `() {
        // Setup the test build
        val projectDir = tempFolder.newFolder("pluginWithMinimalConfig")
        val projectName = "Fake Thing"
        projectDir.mkdirs()
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText("""
            |plugins {
            |   id('io.ia.sdk.modl')
            |}
            |
            |version = "0.0.1"
            |
            |ignitionModule {
            |    id = "com.fake.module"
            |    name = "$projectName"
            |    moduleVersion = version
            |
            |}
        """.trimMargin("|"))

        val result: BuildResult = runTask(projectDir, "tasks")

        assertTrue(result.output.contains("BUILD SUCCESSFUL in"))
    }

    @Test
    fun `can apply plugin successfully with java plugin`() {
        // Setup the test build
        val projectDir = tempFolder.newFolder("withJavaPlugin")
        val projectName = "Fake Thing"
        projectDir.mkdirs()
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText("""
            |plugins {
            |    id('java')
            |    id('io.ia.sdk.modl')
            |}
            |
            |version = "0.0.1"
            |
            |ignitionModule {
            |    id = "com.fake.module"
            |    name = "$projectName"
            |    moduleVersion = version
            |
            |}
        """.trimMargin("|"))

        prepareSigningTestResources(projectDir.toPath().resolve(nameToDirName(projectName)))
        // Run the build
        val result: BuildResult = runTask(projectDir, "tasks")

        // if tasks task was successful, plugin was applied successfully
        assertTrue(result.output.contains("BUILD SUCCESSFUL in"))
    }

    @Test
    fun `can apply plugin successfully with java-library plugin`() {
        // Setup the test build
        val projectDir = tempFolder.newFolder("withJavaLibraryPlugin")
        val projectName = "Fake Thing"
        projectDir.mkdirs()
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText("""
            |plugins {
            |    id('java-library')
            |    id('io.ia.sdk.modl')
            |}
            |
            |
            |version = "0.0.1"
            |
            |ignitionModule {
            |    id = "com.fake.module"
            |    name = "$projectName"
            |    moduleVersion = version
            |
            |}
        """.trimMargin("|"))

        prepareSigningTestResources(projectDir.toPath().resolve(nameToDirName(projectName)))
        // Run the build
        val result: BuildResult = runTask(projectDir, "tasks")

        assertTrue(result.output.contains("BUILD SUCCESSFUL in"))
    }

    private fun propFileLocation(testFolder: Path, moduleName: String): String {
        val dirName = nameToDirName(moduleName)
        val propFile = testFolder.resolve(dirName).resolve("signing.properties")

        return "project.file(\"${propFile.toFile().absolutePath}\")"
    }

    @Test
    fun `gateway scoped module passes config and builds`() {
        val rootDir = tempFolder.newFolder("gwScopedConfigAndBuild").toPath()
        val moduleName = "Great Tests"
        val scopes = "G"
        val packageName = "le.examp"

        val config: GeneratorConfig = GeneratorConfig.ConfigBuilder()
            .moduleName(moduleName)
            .scopes(scopes)
            .packageName(packageName)
            .parentDir(rootDir)
            .signingCredentialPropertyFile(propFileLocation(rootDir, moduleName))
            .build()

        val projectDir = ModuleGenerator.generate(config)
        prepareSigningTestResources(rootDir.resolve(nameToDirName(moduleName)))

        runTask(projectDir.toFile(), "build")
        val expected = expectedSignedModule(projectDir, moduleName)
        println("Expected: ${expected.absolutePath}")

        assertTrue(expected.exists(), "Built and signed module exists")
    }

    private fun expectedSignedModule(rootModuleDir: Path, moduleName: String): File {
        val expectedLocation = rootModuleDir.resolve("build").toAbsolutePath()
        return File("$expectedLocation/${signedModuleName(moduleName)}")
    }

    @Test
    fun `GC scoped module spits out into build dir`() {
        val rootDir = tempFolder.newFolder("gcScopedModule").toPath()
        val moduleName = "Great Tests Gateway Client"
        val scopes = "GC"
        val packageName = "bot.skynet"

        val config: GeneratorConfig = GeneratorConfig.ConfigBuilder()
            .moduleName(moduleName)
            .scopes(scopes)
            .packageName(packageName)
            .signingCredentialPropertyFile(propFileLocation(rootDir, moduleName))
            .parentDir(rootDir)
            .build()

        val projectDir = ModuleGenerator.generate(config)
        prepareSigningTestResources(rootDir.resolve(nameToDirName(moduleName)))

        runTask(projectDir.toFile(), "build")
        assertTrue(expectedSignedModule(projectDir, moduleName).exists(), "GC signed module detected")
    }

    @Test
    fun `GCD scoped module spits out into build dir`() {
        val rootDir = tempFolder.newFolder("gcdScopedModule").toPath()
        val moduleName = "Great Tests With GCD Scope"
        val scopes = "GCD"
        val packageName = "bot.skynet.wow"

        if (Files.exists(rootDir)) {
            rootDir.toFile().deleteRecursively()
        }

        rootDir.toFile().mkdirs()

        val config: GeneratorConfig = GeneratorConfig.ConfigBuilder()
            .moduleName(moduleName)
            .scopes(scopes)
            .packageName(packageName)
            .signingCredentialPropertyFile(propFileLocation(rootDir, nameToDirName(moduleName)))
            .parentDir(rootDir)
            .build()

        val projectDir = ModuleGenerator.generate(config)
        prepareSigningTestResources(rootDir.resolve(nameToDirName(moduleName)))

        runTask(projectDir.toFile(), "build")
        listOf("client", "designer", "common", "gateway").forEach {
            assertTrue(File(projectDir.toFile().absolutePath, it).exists(), "child dir exists as expected")
        }
        assertTrue(expectedSignedModule(projectDir, moduleName).exists(), "GC signed module detected")
    }

    @Test
    fun `GCD scoped module with debug plugin config`() {
        val rootDir = tempFolder.newFolder("gcdDebugPluginConfig").toPath()
        val moduleName = "Biological Dispatcher"
        val scopes = "GCD"
        val packageName = "bot.skynet.wow"

        val config: GeneratorConfig = GeneratorConfig.ConfigBuilder()
            .moduleName(moduleName)
            .scopes(scopes)
            .packageName(packageName)
            .parentDir(rootDir)
            .debugPluginConfig(true)
            .rootPluginConfig("   id('io.ia.sdk.modl') version('0.0.1-SNAPSHOT')")
            .signingCredentialPropertyFile(propFileLocation(rootDir, moduleName))
            .build()

        val projectDir = ModuleGenerator.generate(config)
        prepareSigningTestResources(rootDir.resolve(nameToDirName(moduleName)))

        runTask(projectDir.toFile(), "build")
        assertTrue(expectedSignedModule(projectDir, moduleName).exists(), "GC signed module detected")
    }

    private fun build(config: GeneratorConfig) {
        val projectDir = ModuleGenerator.generate(config)

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("build")
        runner.withProjectDir(projectDir.toFile())
        val result = runner.build()

        println("Result: ${result.output}")
    }
}
