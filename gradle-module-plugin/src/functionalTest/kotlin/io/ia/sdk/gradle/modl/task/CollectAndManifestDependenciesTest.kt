package io.ia.sdk.gradle.modl.task

import io.ia.ignition.module.generator.ModuleGenerator
import io.ia.ignition.module.generator.api.GeneratorConfigBuilder
import io.ia.sdk.gradle.modl.BaseTest
import io.ia.sdk.gradle.modl.model.ArtifactManifest
import io.ia.sdk.gradle.modl.model.artifactManifestFromJson
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CollectAndManifestDependenciesTest : BaseTest() {
    companion object {
        const val PATH_KEY = "<FILEPATH>"
        const val PROP_FILE_PATH_FORMAT = "project.file(\"$PATH_KEY\")"
        const val SYS_PROP_USER_HOME = "\${System.getProperty(\"user.home\")}"
    }

    @Test
    fun `client scoped modlApi artifacts collected successfully`() {
        val projectDir = tempFolder.newFolder("clientScopedArtifacts").toPath()
        val name = "Client Artifact"
        val dependencyEntry = "modlApi('org.jfree:org.jfree.svg:4.1')"
        val customizers = mapOf(CLIENT_DEP to dependencyEntry)

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .scopes("C")
            .packageName("check.my.signage")
            .parentDir(projectDir)
            .useRootForSingleScopeProject(false)
            .customReplacements(customizers)
            .build()

        val project = ModuleGenerator.generate(config)

        runTask(project.toFile(), "collectModlDependencies")

        val clientSubproject = project.resolve("client")
        val clientArtifacts = clientSubproject.resolve("build/artifacts")

        assertTrue(clientArtifacts.toFile().exists() && clientArtifacts.toFile().isDirectory, "is dir and present")

        val manifestPath = clientArtifacts.resolve(CollectModlDependencies.JSON_FILENAME)
        assertTrue(manifestPath.toFile().exists(), "manifest should exist")

        val manifest: ArtifactManifest = artifactManifestFromJson(manifestPath.toFile().readText(Charsets.UTF_8))

        assertTrue(manifest.artifacts.size == 2, "two artifacts found as expected in manifest")
        assertNotNull(manifest.artifacts.find { it.fileName == "org.jfree.svg-4.1.jar" }, "jfree artifact exists")
        assertNotNull(manifest.artifacts.find { it.fileName == "client-0.0.1-SNAPSHOT.jar" }, "client artifact exists")

        val dirContents = clientArtifacts.toFile().listFiles()
        assertTrue(dirContents?.size == 3, "correct number of files in artifacts dir")
        val jars = dirContents?.filter { it.extension == "jar" }
        assertTrue(jars?.size == 2, "correct amount of jars in artifacts dir")
    }

    @Test
    fun `client scoped modlImplementation artifacts collected successfully`() {
        val projectDir = tempFolder.newFolder("clientScopedArtifacts").toPath()
        val name = "Client Artifacts"

        val dependencyEntry = "modlImplementation('org.jfree:org.jfree.svg:4.1')"
        val customizers = mapOf(CLIENT_DEP to dependencyEntry)

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .scopes("C")
            .packageName("check.my.signage")
            .parentDir(projectDir)
            .customReplacements(customizers)
            .useRootForSingleScopeProject(false)
            .build()

        val project = ModuleGenerator.generate(config)

        runTask(project.toFile(), "collectModlDependencies")
        val clientSubproject = project.resolve("client")
        val clientArtifacts = clientSubproject.resolve("build/artifacts")

        assertTrue(clientArtifacts.toFile().exists() && clientArtifacts.toFile().isDirectory, "is dir and present")

        val manifestPath = clientArtifacts.resolve(CollectModlDependencies.JSON_FILENAME)
        assertTrue(manifestPath.toFile().exists(), "manifest exists")

        val manifest: ArtifactManifest = artifactManifestFromJson(manifestPath.toFile().readText(Charsets.UTF_8))

        assertTrue(manifest.artifacts.size == 2, "two artifacts found as expected in manifest")
        assertNotNull(manifest.artifacts.find { it.fileName == "org.jfree.svg-4.1.jar" }, "jfree artifact exists")
        assertNotNull(manifest.artifacts.find { it.fileName == "client-0.0.1-SNAPSHOT.jar" }, "client artifact exists")

        val dirContents = clientArtifacts.toFile().listFiles()
        assertTrue(dirContents?.size == 3, "correct number of files in artifacts dir")
        val jars = dirContents?.filter { it.extension == "jar" }
        assertTrue(jars?.size == 2, "correct amount of jars in artifacts dir")
    }
}
