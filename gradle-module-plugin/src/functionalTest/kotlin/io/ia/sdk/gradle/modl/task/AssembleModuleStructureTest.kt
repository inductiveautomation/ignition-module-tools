package io.ia.sdk.gradle.modl.task

import io.ia.ignition.module.generator.ModuleGenerator
import io.ia.ignition.module.generator.api.GeneratorConfigBuilder
import io.ia.sdk.gradle.modl.BaseTest
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertTrue

open class AssembleModuleStructureTest : BaseTest() {

    @Test
    fun `single file doc is collected`() {
        val projectDir = tempFolder.newFolder(currentMethodName()).toPath()

        val name = "Single Doc"

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .scopes("C")
            .packageName("check.my.signage")
            .parentDir(projectDir)
            .useRootForSingleScopeProject(false)
            .build()

        val project = ModuleGenerator.generate(config)

        // add some example docs
        val docs = project.resolve("docs")
        Files.createDirectories(docs)

        val resourcePath = "docs/singleFile/index.html"
        // copy our sample docs in
        ClassLoader.getSystemResourceAsStream(resourcePath).let { inputStream ->
            if (inputStream == null) {
                throw Exception("Failed to read test resource 'certs/$resourcePath")
            }
            val writeTo = docs.resolve("index.html")
            inputStream.copyTo(writeTo.toFile().outputStream(), 1024)

            writeTo
        }

        // amend the plugin configuration to enable the docs
        val rootBuildScript = project.resolve("build.gradle").toFile()

        val buildScriptContents = rootBuildScript.readText().replace(
            "// documentationFiles.from(project.file(\"src/docs/\"))",
            "documentationFiles.from(project.file(\"docs/\"))"
        ).replace(
            "// documentationIndex.set(\"index.html\")",
            "documentationIndex.set(\"index.html\")"
        )
        rootBuildScript.writeText(buildScriptContents)

        runTask(project.toFile(), "assembleModlStructure")
        assertTrue(
            project.resolve("build/moduleContent/doc/index.html").toFile().exists(),
            "doc should exist in staging dir"
        )
    }

    @Test
    fun `multi file docs are collected`() {
        val projectDir = tempFolder.newFolder(currentMethodName()).toPath()
        val name = "Multi Doc"

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .scopes("C")
            .packageName("check.my.signage")
            .parentDir(projectDir)
            .useRootForSingleScopeProject(false)
            .build()

        val project = ModuleGenerator.generate(config)

        // add some example docs
        val docs = project.resolve("docs")
        Files.createDirectories(docs)

        val resourceNames = setOf("index.html", "linked.html")
        // copy our sample docs in
        resourceNames.forEach { fileName ->
            ClassLoader.getSystemResourceAsStream("docs/multipleFiles/$fileName").let { inputStream ->
                if (inputStream == null) {
                    throw Exception("Failed to read test resource 'certs/$")
                }
                val writeTo = docs.resolve(fileName)
                inputStream.copyTo(writeTo.toFile().outputStream())
            }
        }

        // amend the plugin configuration to enable the docs
        val rootBuildScript = project.resolve("build.gradle").toFile()

        val buildScriptContents = rootBuildScript.readText().replace(
            "// documentationFiles.from(project.file(\"src/docs/\"))",
            "documentationFiles.from(project.file(\"docs/\"))"
        ).replace(
            "// documentationIndex.set(\"index.html\")",
            "documentationIndex.set(\"index.html\")"
        )
        rootBuildScript.writeText(buildScriptContents)

        runTask(project.toFile(), "assembleModlStructure")
        assertTrue(
            project.resolve("build/moduleContent/doc/index.html").toFile().exists(),
            "index doc file should exist in staging dir"
        )
        assertTrue(
            project.resolve("build/moduleContent/doc/linked.html").toFile().exists(),
            "linked doc file should exist in staging dir"
        )
    }
}
