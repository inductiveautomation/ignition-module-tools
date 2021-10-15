package io.ia.sdk.gradle.modl.task

import io.ia.ignition.module.generator.ModuleGenerator
import io.ia.ignition.module.generator.api.GeneratorConfigBuilder
import io.ia.sdk.gradle.modl.BaseTest
import io.ia.sdk.gradle.modl.model.ArtifactManifest
import io.ia.sdk.gradle.modl.model.artifactManifestFromJson
import org.junit.Test
import kotlin.test.assertFalse
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
        assertNotNull(manifest.artifacts.find { it.jarName == "org.jfree.svg-4.1.jar" }, "jfree artifact exists")
        assertNotNull(manifest.artifacts.find { it.jarName == "client-0.0.1-SNAPSHOT.jar" }, "client artifact exists")

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
        assertNotNull(manifest.artifacts.find { it.jarName == "org.jfree.svg-4.1.jar" }, "jfree artifact exists")
        assertNotNull(manifest.artifacts.find { it.jarName == "client-0.0.1-SNAPSHOT.jar" }, "client artifact exists")

        val dirContents = clientArtifacts.toFile().listFiles()
        assertTrue(dirContents?.size == 3, "correct number of files in artifacts dir")
        val jars = dirContents?.filter { it.extension == "jar" }
        assertTrue(jars?.size == 2, "correct amount of jars in artifacts dir")
    }

    @Test
    fun `client scoped modlImplementation dependencies result in proper scope entry in manifest`() {
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
        assertNotNull(manifest.artifacts.find { it.jarName == "org.jfree.svg-4.1.jar" }, "jfree artifact exists")
        assertNotNull(manifest.artifacts.find { it.jarName == "client-0.0.1-SNAPSHOT.jar" }, "client artifact exists")

        val dirContents = clientArtifacts.toFile().listFiles()
        assertTrue(dirContents?.size == 3, "correct number of files in artifacts dir")
        val jars = dirContents?.filter { it.extension == "jar" }
        assertTrue(jars?.size == 2, "correct amount of jars in artifacts dir")
    }

    @Test
    fun `multi scoped modlApi artifacts collected successfully with transitives`() {
        val projectDir = tempFolder.newFolder("multiModlApiArtifacts").toPath()
        val name = "Multiscope Artifact"
        val jfreeLib = "modlApi('org.jfree:org.jfree.svg:4.1')"
        val milo = "modlApi('org.eclipse.milo:sdk-server:0.6.1')"
        val customizers = mapOf(
            CLIENT_DEP to jfreeLib,
            GW_DEP to milo
        )

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .scopes("CGD")
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

        val gwSubproject = project.resolve("gateway")
        val gwArtifacts = gwSubproject.resolve("build/artifacts")

        val expectedMiloDependencies = listOf(
            "bcpkix-jdk15on-1.61.jar",
            "bcprov-jdk15on-1.61.jar",
            "bsd-core-0.6.1.jar",
            "bsd-generator-0.6.1.jar",
            "guava-26.0-jre.jar",
            "istack-commons-runtime-3.0.11.jar",
            "jakarta.activation-1.2.2.jar",
            "jakarta.xml.bind-api-2.3.3.jar",
            "jaxb-runtime-2.3.3.jar",
            "netty-buffer-4.1.54.Final.jar",
            "netty-codec-4.1.54.Final.jar",
            "netty-codec-http-4.1.54.Final.jar",
            "netty-common-4.1.54.Final.jar",
            "netty-handler-4.1.54.Final.jar",
            "netty-resolver-4.1.54.Final.jar",
            "netty-transport-4.1.54.Final.jar",
            "sdk-core-0.6.1.jar",
            "sdk-server-0.6.1.jar",
            "slf4j-api-1.7.25.jar",
            "stack-core-0.6.1.jar",
            "stack-server-0.6.1.jar",
            "txw2-2.3.3.jar",
        )

        expectedMiloDependencies.forEach {
            assertTrue(gwArtifacts.resolve(it).toFile().exists(), "Artifact $it should exist in artifacts dir.")
        }

        val manifestPath = clientArtifacts.resolve(CollectModlDependencies.JSON_FILENAME)
        assertTrue(manifestPath.toFile().exists(), "manifest should exist")

        val manifest: ArtifactManifest = artifactManifestFromJson(manifestPath.toFile().readText(Charsets.UTF_8))

        assertTrue(manifest.artifacts.size == 2, "two artifacts found as expected in manifest")
        assertNotNull(manifest.artifacts.find { it.jarName == "org.jfree.svg-4.1.jar" }, "jfree artifact exists")
        assertNotNull(manifest.artifacts.find { it.jarName == "client-0.0.1-SNAPSHOT.jar" }, "client artifact exists")

        val dirContents = clientArtifacts.toFile().listFiles()
        assertTrue(dirContents?.size == 3, "correct number of files in artifacts dir")
        val jars = dirContents?.filter { it.extension == "jar" }
        assertTrue(jars?.size == 2, "correct amount of jars in artifacts dir")
    }

    @Test
    fun `multi scoped modlImplementation artifacts and transitives correctly collected`() {
        val projectDir = tempFolder.newFolder("multiModlImplArtifacts").toPath()
        val name = "Multiscope Impl Artifacts"
        val jfreeLib = "modlApi('org.jfree:org.jfree.svg:4.1')"
        val milo = "modlImplementation('org.eclipse.milo:sdk-server:0.6.1')"
        val customizers = mapOf(
            CLIENT_DEP to jfreeLib,
            GW_DEP to milo
        )

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .scopes("CGD")
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

        val gwSubproject = project.resolve("gateway")
        val gwArtifacts = gwSubproject.resolve("build/artifacts")

        val expectedMiloDependencies = listOf(
            "bcpkix-jdk15on-1.61.jar",
            "bcprov-jdk15on-1.61.jar",
            "bsd-core-0.6.1.jar",
            "bsd-generator-0.6.1.jar",
            "guava-26.0-jre.jar",
            "istack-commons-runtime-3.0.11.jar",
            "jakarta.activation-1.2.2.jar",
            "jakarta.xml.bind-api-2.3.3.jar",
            "jaxb-runtime-2.3.3.jar",
            "netty-buffer-4.1.54.Final.jar",
            "netty-codec-4.1.54.Final.jar",
            "netty-codec-http-4.1.54.Final.jar",
            "netty-common-4.1.54.Final.jar",
            "netty-handler-4.1.54.Final.jar",
            "netty-resolver-4.1.54.Final.jar",
            "netty-transport-4.1.54.Final.jar",
            "sdk-core-0.6.1.jar",
            "sdk-server-0.6.1.jar",
            "slf4j-api-1.7.25.jar",
            "stack-core-0.6.1.jar",
            "stack-server-0.6.1.jar",
            "txw2-2.3.3.jar",
        )

        expectedMiloDependencies.forEach {
            assertTrue(gwArtifacts.resolve(it).toFile().exists(), "Artifact $it should exist in artifacts dir.")
        }

        val manifestPath = clientArtifacts.resolve(CollectModlDependencies.JSON_FILENAME)
        assertTrue(manifestPath.toFile().exists(), "manifest should exist")

        val manifest: ArtifactManifest = artifactManifestFromJson(manifestPath.toFile().readText(Charsets.UTF_8))

        assertTrue(manifest.artifacts.size == 2, "two artifacts found as expected in manifest")
        assertNotNull(manifest.artifacts.find { it.jarName == "org.jfree.svg-4.1.jar" }, "jfree artifact exists")
        assertNotNull(manifest.artifacts.find { it.jarName == "client-0.0.1-SNAPSHOT.jar" }, "client artifact exists")

        val dirContents = clientArtifacts.toFile().listFiles()
        assertTrue(dirContents?.size == 3, "correct number of files in artifacts dir")
        val jars = dirContents?.filter { it.extension == "jar" }
        assertTrue(jars?.size == 2, "correct amount of jars in artifacts dir")
    }

    @Test
    fun `multi scoped modlImplementation artifacts correctly omit exclusions`() {
        val projectDir = tempFolder.newFolder("multiModlImplArtifacts").toPath()
        val name = "Multiscope Impl Artifacts"
        val jfreeLib = "modlApi('org.jfree:org.jfree.svg:4.1')"
        val milo = """
            modlImplementation('org.eclipse.milo:sdk-server:0.6.1') {
                exclude(module: "stack-core")
            }
            """
        val customizers = mapOf(
            CLIENT_DEP to jfreeLib,
            GW_DEP to milo
        )

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .scopes("CGD")
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

        val gwSubproject = project.resolve("gateway")
        val gwArtifacts = gwSubproject.resolve("build/artifacts")

        val expectedMiloDependencies = listOf(
            "bsd-core-0.6.1.jar",
            "bsd-generator-0.6.1.jar",
            "FastInfoset-1.2.16.jar",
            "gateway-0.0.1-SNAPSHOT.jar",
            "netty-buffer-4.1.54.Final.jar",
            "netty-codec-4.1.54.Final.jar",
            "netty-codec-http-4.1.54.Final.jar",
            "netty-common-4.1.54.Final.jar",
            "netty-handler-4.1.54.Final.jar",
            "netty-resolver-4.1.54.Final.jar",
            "netty-transport-4.1.54.Final.jar",
            "sdk-core-0.6.1.jar",
            "sdk-server-0.6.1.jar",
            "stack-server-0.6.1.jar",
        )

        expectedMiloDependencies.forEach {
            assertTrue(gwArtifacts.resolve(it).toFile().exists(), "Artifact $it should exist in artifacts dir.")
        }

        // exclusions include those we specify AND their transitives
        val excluded = listOf(
            "bcpkix-jdk15on-1.61.jar",
            "bcprov-jdk15on-1.61.jar",
            "guava-26.0-jre.jar",
            "istack-commons-runtime-3.0.11.jar",
            "jakarta.activation-1.2.2.jar",
            "jakarta.xml.bind-api-2.3.3.jar",
            "jaxb-runtime-2.3.3.jar",
            "slf4j-api-1.7.25.jar",
            "stack-core-0.6.1.jar",
            "txw2-2.3.3.jar",
        )

        excluded.forEach {
            assertFalse(
                gwArtifacts.resolve(it).toFile().exists(),
                "Transitive artifact $it should not have been collected."
            )
        }

        val manifestPath = clientArtifacts.resolve(CollectModlDependencies.JSON_FILENAME)
        assertTrue(manifestPath.toFile().exists(), "manifest should exist")

        val manifest: ArtifactManifest = artifactManifestFromJson(manifestPath.toFile().readText(Charsets.UTF_8))

        assertTrue(manifest.artifacts.size == 2, "two artifacts found as expected in manifest")
        assertNotNull(manifest.artifacts.find { it.jarName == "org.jfree.svg-4.1.jar" }, "jfree artifact exists")
        assertNotNull(manifest.artifacts.find { it.jarName == "client-0.0.1-SNAPSHOT.jar" }, "client artifact exists")

        val dirContents = clientArtifacts.toFile().listFiles()
        assertTrue(dirContents?.size == 3, "correct number of files in artifacts dir")
        val jars = dirContents?.filter { it.extension == "jar" }
        assertTrue(jars?.size == 2, "correct amount of jars in artifacts dir")
    }
}
