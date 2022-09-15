package io.ia.ignition.module.generator

import io.ia.ignition.module.generator.api.GeneratorConfigBuilder
import io.ia.ignition.module.generator.api.GradleDsl
import io.ia.ignition.module.generator.api.TemplateMarker
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
            val config = GeneratorConfigBuilder()
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

        val config = GeneratorConfigBuilder()
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

        val config = GeneratorConfigBuilder()
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

    @Test
    fun `G project has appropriate module configuration`() {
        val parentDir = tempFolder.newFolder().toPath()

        val scopes = "G"
        val pkg = "bot.skynet.terminator"
        val name = "T Two Hundred"

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .packageName(pkg)
            .parentDir(parentDir)
            .scopes(scopes)
            .useRootForSingleScopeProject(false)
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

        val expected = "    hooks = [\n" +
            "        \"bot.skynet.terminator.gateway.TTwoHundredGatewayHook\" : \"G\"\n" +
            "    ]"
        val rootBuildFile = projDir.resolve("build.gradle")
        val content = rootBuildFile.toFile().readText(Charsets.UTF_8)
        assertTrue(Files.exists(rootBuildFile))
        assertTrue(content.contains(expected))
    }

    @Test
    fun `G single dir project has appropriate module and dependency configuration`() {
        val parentDir = tempFolder.newFolder().toPath()

        val scopes = "G"
        val pkg = "bot.skynet.terminator"
        val name = "T Two Hundred"

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .packageName(pkg)
            .parentDir(parentDir)
            .scopes(scopes)
            .useRootForSingleScopeProject(true)
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

        val expected = "    hooks = [\n" +
            "        \"bot.skynet.terminator.gateway.TTwoHundredGatewayHook\" : \"G\"\n" +
            "    ]"
        val rootBuildFile = projDir.resolve("build.gradle")
        val content = rootBuildFile.toFile().readText(Charsets.UTF_8)
        assertTrue(Files.exists(rootBuildFile))
        assertTrue(content.contains(expected))
        assertTrue(content.contains("dependencies {"))
        val gwDeps = """
            |    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:${'$'}sdk_version")
            |    compileOnly("com.inductiveautomation.ignitionsdk:gateway-api:${'$'}sdk_version")
        """.trimMargin()
        assertTrue(content.contains(gwDeps), "buildscript should include '$gwDeps'")
    }

    @Test
    fun `G single dir kotlin project has appropriate module and dependency configuration`() {
        val parentDir = tempFolder.newFolder().toPath()

        val scopes = "G"
        val pkg = "bot.skynet.terminator"
        val name = "T Two Hundred"

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .packageName(pkg)
            .parentDir(parentDir)
            .scopes(scopes)
            .buildscriptDsl(GradleDsl.KOTLIN)
            .useRootForSingleScopeProject(true)
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

        val expected = """
            |    hooks.putAll(mapOf(
            |        "bot.skynet.terminator.gateway.TTwoHundredGatewayHook" to "G"
            |    ))
        """.trimMargin()
        val rootBuildFile = projDir.resolve("build.gradle.kts")
        val content = rootBuildFile.toFile().readText(Charsets.UTF_8)
        assertTrue(Files.exists(rootBuildFile))
        assertTrue(content.contains(expected))
        assertTrue(content.contains("dependencies {"))
        val gwDeps = """
            |    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:${'$'}{rootProject.extra["sdk_version"]}")
            |    compileOnly("com.inductiveautomation.ignitionsdk:gateway-api:${'$'}{rootProject.extra["sdk_version"]}")
        """.trimMargin()
        assertTrue(content.contains(gwDeps), "buildscript should include '$gwDeps'")
    }

    @Test
    fun `CG groovy project has appropriate module configuration`() {
        val parentDir = tempFolder.newFolder().toPath()

        val scopes = "GC"
        val pkg = "bot.skynet.terminator"
        val name = "T Two Hundred"

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .packageName(pkg)
            .parentDir(parentDir)
            .scopes(scopes)
            .buildscriptDsl(GradleDsl.GROOVY)
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

        val expected = "    hooks = [\n" +
            "        \"bot.skynet.terminator.gateway.TTwoHundredGatewayHook\" : \"G\",\n" +
            "        \"bot.skynet.terminator.client.TTwoHundredClientHook\" : \"C\"\n" +
            "    ]"
        val rootBuildFile = projDir.resolve("build.gradle")
        val content = rootBuildFile.toFile().readText(Charsets.UTF_8)
        assertTrue(Files.exists(rootBuildFile))
        assertTrue(content.contains(expected))
    }

    @Test
    fun `CG kotlin project has appropriate hook configuration`() {
        val parentDir = tempFolder.newFolder().toPath()

        val scopes = "GC"
        val pkg = "bot.skynet.terminator"
        val name = "T Two Hundred"

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .packageName(pkg)
            .parentDir(parentDir)
            .scopes(scopes)
            .buildscriptDsl(GradleDsl.KOTLIN)
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

        val expected = """
        |    hooks.putAll(mapOf(
        |        "bot.skynet.terminator.gateway.TTwoHundredGatewayHook" to "G",
        |        "bot.skynet.terminator.client.TTwoHundredClientHook" to "C"
        |    ))
        """.trimMargin()
        val rootBuildFile = projDir.resolve("build.gradle.kts")
        val content = rootBuildFile.toFile().readText(Charsets.UTF_8)
        assertTrue(Files.exists(rootBuildFile))
        println(content)
        assertTrue(content.contains(expected))
    }

    @Test
    fun `CGD groovy project has appropriate module configuration`() {
        val parentDir = tempFolder.newFolder().toPath()

        val scopes = "GCD"
        val pkg = "bot.skynet.terminator"
        val name = "T Two Hundred"

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .packageName(pkg)
            .parentDir(parentDir)
            .scopes(scopes)
            .buildscriptDsl(GradleDsl.GROOVY)
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

        val expected = "    hooks = [\n" +
            "        \"bot.skynet.terminator.gateway.TTwoHundredGatewayHook\" : \"G\",\n" +
            "        \"bot.skynet.terminator.client.TTwoHundredClientHook\" : \"C\",\n" +
            "        \"bot.skynet.terminator.designer.TTwoHundredDesignerHook\" : \"D\"\n" +
            "    ]"
        val rootBuildFile = projDir.resolve("build.gradle")
        val content = rootBuildFile.toFile().readText(Charsets.UTF_8)
        assertTrue(Files.exists(rootBuildFile))
        assertTrue(content.contains(expected))
    }

    @Test
    fun `CGD kotlin project has appropriate hook configuration`() {
        val parentDir = tempFolder.newFolder().toPath()

        val scopes = "GCD"
        val pkg = "bot.skynet.terminator"
        val name = "T Two Hundred"

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .packageName(pkg)
            .parentDir(parentDir)
            .scopes(scopes)
            .buildscriptDsl(GradleDsl.KOTLIN)
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

        val expected = """
        |    hooks.putAll(mapOf(
        |        "bot.skynet.terminator.gateway.TTwoHundredGatewayHook" to "G",
        |        "bot.skynet.terminator.client.TTwoHundredClientHook" to "C",
        |        "bot.skynet.terminator.designer.TTwoHundredDesignerHook" to "D"
        |    ))
        """.trimMargin()
        val rootBuildFile = projDir.resolve("build.gradle.kts")
        val content = rootBuildFile.toFile().readText(Charsets.UTF_8)
        assertTrue(Files.exists(rootBuildFile))
        assertTrue(content.contains(expected))
    }

    @Test
    fun `G scoped single directory project builds without error`() {
        val parentDir = tempFolder.newFolder("gScopedSingleDirProjBuildsWithoutError").toPath()

        if (Files.exists(parentDir)) {
            parentDir.toFile().deleteRecursively()
        }

        Files.createDirectories(parentDir)

        val scopes = "G"
        val pkg = "is.simple"
        val name = "Only One Scope"

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .packageName(pkg)
            .parentDir(parentDir)
            .scopes(scopes)
            .useRootForSingleScopeProject(true)
            .buildscriptDsl(GradleDsl.GROOVY)
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

        val rootBuildFile = projDir.resolve("build.gradle")
        assertTrue(Files.exists(rootBuildFile))

        val content = rootBuildFile.toFile().readText(Charsets.UTF_8)
        assertTrue(content.contains("java-library"), "java library applied as it should")
        assertTrue(projDir.resolve("src/main/java").toFile().exists(), "java source dir exists")
    }
}
