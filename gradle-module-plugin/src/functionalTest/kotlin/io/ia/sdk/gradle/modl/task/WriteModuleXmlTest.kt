package io.ia.sdk.gradle.modl.task

import io.ia.ignition.module.generator.ModuleGenerator
import io.ia.ignition.module.generator.api.GeneratorConfigBuilder
import io.ia.ignition.module.generator.api.GradleDsl
import io.ia.sdk.gradle.modl.BaseTest
import io.ia.sdk.gradle.modl.util.collapseXmlToOneLine
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class WriteModuleXmlTest : BaseTest() {
    companion object {
        const val MODULE_NAME = "ModuleXmlTest"
        const val PACKAGE_NAME = "module.xml.test"
        const val DEPENDS = "<depends"
    }

    @Test
    // @Tag("IGN-9137")
    fun `single module dependency marked as not required`() {
        val dirName = currentMethodName()
        val replacements = mapOf(
            "moduleDependencySpecs = [ ]" to
                """
    moduleDependencySpecs = [
        moduleId("io.ia.modl") {
            it.scope = "GCD"
            it.required = false
        }
    ]
                """,
            "requiredIgnitionVersion = \"8.0.10\"" to
                "requiredIgnitionVersion = \"8.3.0\""
        )

        val oneLineXml = generateXml(dirName, replacements)

        assertContains(
            oneLineXml,
            """<depends scope="GCD" required="false">io.ia.modl</depends>"""
        )
        assertEquals(
            Regex(DEPENDS).findAll(oneLineXml).toList().size,
            1
        )
    }

    @Test
    // @Tag("IGN-9137")
    fun `multiple module dependencies marked as required`() {
        val dirName = currentMethodName()
        val replacements = mapOf(
            "moduleDependencySpecs = [ ]" to
                """
    moduleDependencySpecs = [
        moduleId("io.ia.modl") {
            it.scope = "GCD"
            it.required = true
        },
        moduleId("io.ia.otherModl") {
            it.scope = "G"
            it.required = true
        }
    ]
                """,
            "requiredIgnitionVersion = \"8.0.10\"" to
                "requiredIgnitionVersion = \"8.3.0\""
        )

        val oneLineXml = generateXml(dirName, replacements)

        assertContains(
            oneLineXml,
            """<depends scope="GCD" required="true">io.ia.modl</depends>"""
        )
        assertContains(
            oneLineXml,
            """<depends scope="G" required="true">io.ia.otherModl</depends>"""
        )
        assertEquals(
            Regex(DEPENDS).findAll(oneLineXml).toList().size,
            2
        )
    }

    @Test
    // @Tag("IGN-9137")
    fun `legacy module dependencies not marked at all for requiredness`() {
        val dirName = currentMethodName()

        val replacements = mapOf(
            "moduleDependencies = [ : ]" to
                "moduleDependencies = ['io.ia.modl': 'GCD']"
        )

        val oneLineXml = generateXml(dirName, replacements)

        assertContains(
            oneLineXml,
            """<depends scope="GCD">io.ia.modl</depends>"""
        )
        assertEquals(
            Regex(DEPENDS).findAll(oneLineXml).toList().size,
            1
        )
    }

    private fun generateModule(
        projDir: File,
        replacements: Map<String, String> = mapOf(),
    ): Path {
        val config = GeneratorConfigBuilder()
            .moduleName(MODULE_NAME)
            .scopes("GCD")
            .packageName(PACKAGE_NAME)
            .parentDir(projDir.toPath())
            .customReplacements(replacements)
            .debugPluginConfig(true)
            .allowUnsignedModules(true)
            .settingsDsl(GradleDsl.GROOVY)
            .rootPluginConfig(
                """
                    id("io.ia.sdk.modl")
                """.trimIndent()
            )
            .build()

        return ModuleGenerator.generate(config)
    }

    private fun generateXml(
        dirName: String,
        replacements: Map<String, String> = mapOf()
    ): String {
        val projectDir = generateModule(
            tempFolder.newFolder(dirName),
            replacements,
        )

        val result: BuildResult = runTask(
            projectDir.toFile(),
            listOf(
                "writeModuleXml",
                "--stacktrace",
            )
        )

        val task = result.task(":writeModuleXml")
        assertEquals(task?.outcome, TaskOutcome.SUCCESS)

        // We could do real XML parsing here but this is just a test,
        // quick-and-dirty should be fine.
        return collapseXmlToOneLine(
            projectDir.resolve("build/moduleContent/module.xml").readText()
        )
    }
}
