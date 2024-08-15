package io.ia.sdk.gradle.modl.task

import io.ia.ignition.module.generator.ModuleGenerator
import io.ia.ignition.module.generator.api.GeneratorConfigBuilder
import io.ia.ignition.module.generator.api.GradleDsl
import io.ia.ignition.module.generator.api.ProjectScope
import io.ia.ignition.module.generator.util.replacePlaceholders
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
            "moduleDependencySpecs { }" to
                """
    moduleDependencySpecs {
        register("io.ia.modl") {
            scope = "GCD"
            required = false
        }
    }
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
            "moduleDependencySpecs { }" to
                """
    moduleDependencySpecs {
        register("io.ia.modl") {
            scope = "GCD"
            required = true
        }
        register("io.ia.otherModl") {
            scope = "G"
            required = true
        }
    }
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
    fun `module dependencies via compact, eager DSL`() {
        val dirName = currentMethodName()

        // This allows for streamlined, magical build scripts but there is a
        // slight performance hit as the ModuleDependencySpecs are eagerly
        // created during build script configuration as opposed to registered
        // for lazy configuration only on demand. With `register` as in other
        // tests here and per our guidance in the doc that _should_ only be
        // when `writeModuleXml` task is fired. One can imagine use cases where
        // that task is not fired and this eager instance creation is an
        // unnecessary waste of CPU cycles.
        val replacements = mapOf(
            "moduleDependencySpecs { }" to
                """
    moduleDependencySpecs {
        "io.ia.modl" {
            scope = "GCD"
            required = true
        }
        "io.ia.otherModl" {
            scope = "G"
            required = true
        }
    }
                """,
            "requiredIgnitionVersion = \"8.0.10\"" to
                "requiredIgnitionVersion = \"8.3.0\""
        )

        val oneLineXml = generateXml(
            dirName,
            replacements,
            // true,
        )

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

    @Test
    // @Tag ("IGN-10612")
    fun `jars are included, de-duplicated, and sorted`() {
        val dirName = currentMethodName()
        val dependencies = mapOf<String, String>(
            // JLA-1.5 pulls in commons-math3-3.5 as transitive dep
            "G" to "modlApi 'pl.edu.icm:JLargeArrays:1.5'",
            "D" to "modlApi 'org.duckdb:duckdb_jdbc:0.9.2'",
            // C[lient] implies D[esigner], so here C -> CD
            "C" to "modlApi 'jline:jline:2.12'",
            // Again, here CG -> CDG
            "CG" to "modlApi 'javassist:javassist:3.12.1.GA'",
            // Pulls in commons-pool-1.5.4 as transitive dep
            "DG" to "modlApi 'commons-dbcp:commons-dbcp:1.4'",
            "CD" to "modlApi 'args4j:args4j:2.0.8'",
            "CDG" to "modlApi 'com.inductiveautomation.ignition:ia-gson:2.10.1'",
        )

        val oneLineXml = generateXml(dirName, emptyMap(), dependencies)

        // Split to list on the whitespace between nodes, extract <jar/>s.
        val jars =
            oneLineXml.split(Regex("""(?<=>)\s+(?=<)"""))
                .filter { node -> node.startsWith("<jar") }

        assertEquals(
            jars,
            listOf(
                """<jar scope="CDG">common-0.0.1-SNAPSHOT.jar</jar>""",
                """<jar scope="CDG">ia-gson-2.10.1.jar</jar>""",
                """<jar scope="CDG">javassist-3.12.1.GA.jar</jar>""",
                """<jar scope="CD">args4j-2.0.8.jar</jar>""",
                """<jar scope="CD">client-0.0.1-SNAPSHOT.jar</jar>""",
                """<jar scope="CD">jline-2.12.jar</jar>""",
                """<jar scope="DG">commons-dbcp-1.4.jar</jar>""",
                """<jar scope="DG">commons-pool-1.5.4.jar</jar>""",
                """<jar scope="D">designer-0.0.1-SNAPSHOT.jar</jar>""",
                """<jar scope="D">duckdb_jdbc-0.9.2.jar</jar>""",
                """<jar scope="G">JLargeArrays-1.5.jar</jar>""",
                """<jar scope="G">commons-math3-3.5.jar</jar>""",
                """<jar scope="G">gateway-0.0.1-SNAPSHOT.jar</jar>""",
            )
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
        replacements: Map<String, String> = mapOf(),
        dependencies: Map<String, String> = mapOf(),
        dumpBuildScript: Boolean = false,
    ): String {
        val projectDir = generateModule(
            tempFolder.newFolder(dirName),
            replacements,
        )

        dependencies.forEach { (scopes, dependency) ->
            ProjectScope.scopesFromShorthand(scopes).forEach { scope ->
                val dependenciesString = """
                    dependencies {
                        $dependency
                """.trimIndent()
                projectDir.resolve("${scope.folderName}/build.gradle").replacePlaceholders(
                    mapOf("dependencies {" to dependenciesString)
                )
            }
        }

        if (dumpBuildScript) {
            println("build script:")
            println(projectDir.resolve("build.gradle").readText())
        }

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
