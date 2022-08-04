package io.ia.sdk.gradle.modl.task

import io.ia.ignition.module.generator.ModuleGenerator
import io.ia.ignition.module.generator.api.GeneratorConfigBuilder
import io.ia.sdk.gradle.modl.BaseTest
import io.ia.sdk.gradle.modl.model.AssemblyManifest
import io.ia.sdk.gradle.modl.model.MOSHI
import org.junit.Test
import java.io.File
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

fun jsonToAssemblyManifest(json: String): AssemblyManifest {
    val adapter = MOSHI.adapter(AssemblyManifest::class.java)
    return adapter.fromJson(json)!!
}

class ModuleBuildReportTest : BaseTest() {

    @Test
    fun `build report is generated`() {
        val parentDir: File = tempFolder.newFolder("build_report_generated")
        val moduleName = "I Have Reports"
        val signingResourcesDestination = parentDir.toPath().resolve("i-have-reports")

        prepareSigningTestResources(signingResourcesDestination)

        val config = GeneratorConfigBuilder()
            .moduleName(moduleName)
            .scopes("GCD")
            .packageName("check.my.signage")
            .parentDir(parentDir.toPath())
            .debugPluginConfig(true)
            .rootPluginConfig(
                """
                    id("io.ia.sdk.modl")
                """.trimIndent()
            )
            .build()

        val projectDir = ModuleGenerator.generate(config)

        runTask(projectDir.toFile(), "modlReport")
        val buildDir = projectDir.resolve("build")
        val buildJson = buildDir.resolve("buildResult.json")

        assertTrue(buildJson.toFile().exists(), "buildResult.json exists in the build dir")
    }

    @Test
    fun `build report contains checksum entry`() {
        val parentDir: File = tempFolder.newFolder("build_report_checksum")
        val moduleName = "I Have Checksum"
        val signingResourcesDestination = parentDir.toPath().resolve("i-have-checksum")

        val config = GeneratorConfigBuilder()
            .moduleName(moduleName)
            .scopes("GCD")
            .packageName("check.my.signage")
            .parentDir(parentDir.toPath())
            .debugPluginConfig(true)
            .rootPluginConfig(
                """
                    id("io.ia.sdk.modl")
                """.trimIndent()
            )
            .build()

        val projectDir = ModuleGenerator.generate(config)
        prepareSigningTestResources(signingResourcesDestination)

        runTask(projectDir.toFile(), "modlReport")
        val buildDir = projectDir.resolve("build")
        val buildJson = buildDir.resolve("buildResult.json")

        assertTrue(buildJson.toFile().exists(), "buildResult.json exists in the build dir")
        val report = jsonToAssemblyManifest(buildJson.toFile().readText())
        assertNotNull(report.checksum, "checksum entry was present")
    }

    @Test
    fun `build report contains metainfo entries`() {
        val parentDir: File = tempFolder.newFolder("build_report_checksum")
        val moduleName = "I Have Checksum"
        val signingResourcesDestination = parentDir.toPath().resolve("i-have-checksum")

        val metainfoEntry = """metaInfo.put("test.key", "some string value")"""
        val replacement = mapOf("ignitionModule {" to "ignitionModule {\n    ${metainfoEntry}\n")
        val config = GeneratorConfigBuilder()
            .moduleName(moduleName)
            .scopes("GCD")
            .packageName("check.my.signage")
            .parentDir(parentDir.toPath())
            .debugPluginConfig(true)
            .rootPluginConfig(
                """
                    id("io.ia.sdk.modl")
                """.trimIndent()
            )
            .customReplacements(replacement)
            .build()

        val projectDir = ModuleGenerator.generate(config)
        prepareSigningTestResources(signingResourcesDestination)

        runTask(projectDir.toFile(), "modlReport")
        val buildDir = projectDir.resolve("build")
        val buildJson = buildDir.resolve("buildResult.json")

        assertTrue(buildJson.toFile().exists(), "buildResult.json exists in the build dir")
        val report = jsonToAssemblyManifest(buildJson.toFile().readText())
        assertEquals(1, report.metaInfo.size, "metainfo should be added to report if configured in plugin")
        assertEquals("some string value", report.metaInfo["test.key"], "build report should hold metainfo value")
    }

    @Test
    fun `build report contains unsigned modl entry when skipModlSigning`() {
        val parentDir: File = tempFolder.newFolder("build_report_unsigned")
        val moduleName = "Foo"

        val config = GeneratorConfigBuilder()
            .moduleName(moduleName)
            .scopes("GCD")
            .packageName("check.my.signage")
            .parentDir(parentDir.toPath())
            .debugPluginConfig(true)
            .rootPluginConfig(
                """
                    id("io.ia.sdk.modl")
                """.trimIndent()
            )
            .build()

        val projectDir = ModuleGenerator.generate(config)
        projectDir.resolve("build.gradle").toFile().let {
            it.writeText(
                it.readLines().map { line ->
                    if ("    // skipModlSigning = false" == line) {
                        "    skipModlSigning = true"
                    } else {
                        line
                    }
                }.joinToString(System.lineSeparator())
            )
        }

        runTask(projectDir.toFile(), "modlReport")
        val buildDir = projectDir.resolve("build")
        val buildJson = buildDir.resolve("buildResult.json")

        assertTrue(buildJson.toFile().exists(), "buildResult.json should exist in the build dir")
        val report = jsonToAssemblyManifest(buildJson.toFile().readText())
        assertNotNull(report.fileName, "filename entry was not present")
        assertEquals("Foo.unsigned.modl", report.fileName, "filename entry was not present")
    }
}
