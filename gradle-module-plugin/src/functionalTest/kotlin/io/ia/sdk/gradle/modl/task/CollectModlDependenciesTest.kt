package io.ia.sdk.gradle.modl.task

import io.ia.ignition.module.generator.ModuleGenerator
import io.ia.ignition.module.generator.api.GeneratorConfigBuilder
import io.ia.sdk.gradle.modl.BaseTest
import io.ia.sdk.gradle.modl.model.ArtifactManifest
import io.ia.sdk.gradle.modl.model.artifactManifestFromJson
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import java.util.zip.ZipFile.OPEN_READ
import kotlin.io.path.deleteExisting
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollectModlDependenciesTest : BaseTest() {
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

    @Test
    fun `single dir G project emits correct jarfile`() {
        val projectDir = tempFolder.newFolder("rootImplArtifacts").toPath()
        val name = "Single Dir G Proj"

        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .scopes("G")
            .packageName("check.my.signage")
            .parentDir(projectDir)
            .useRootForSingleScopeProject(true)
            .build()

        val project = ModuleGenerator.generate(config)

        runTask(project.toFile(), "collectModlDependencies")

        val artifactsDir = project.resolve("build/artifacts")
        val manifestPath = artifactsDir.resolve(CollectModlDependencies.JSON_FILENAME)
        assertTrue(manifestPath.toFile().exists(), "manifest should exist")

        val manifest: ArtifactManifest = artifactManifestFromJson(manifestPath.toFile().readText(Charsets.UTF_8))

        assertTrue(manifest.artifacts.size == 1, "should be one artifact found in manifest")
        assertTrue(artifactsDir.toFile().listFiles().size == 2, "should have 2 files in artifact dir")
        assertTrue(
            artifactsDir.resolve("single-dir-g-proj-0.0.1-SNAPSHOT.jar").toFile().exists(),
            "versioned jarfile should exist"
        )
    }

    private fun spliceResourceFileIntoProject(srcPath: String, destPath: Path) {
        runCatching {
            ClassLoader.getSystemResourceAsStream(srcPath).use { inp ->
                Files.copy(inp!!, destPath)
            }
        }.onFailure { e ->
            throw Exception("Unable to read or copy test resource '$srcPath'", e)
        }
    }

    @Test
    // @Tag("IGN-6325")
    fun `subproject programmatic source file deletion detected during rebuild`() {
        val projectDir = tempFolder.newFolder("sourceDeletion").toPath()

        val config = GeneratorConfigBuilder()
            .moduleName("Source Deletion")
            .scopes("G")
            .packageName("hot.sources")
            .parentDir(projectDir)
            .useRootForSingleScopeProject(false)
            .build()

        val project = ModuleGenerator.generate(config)
        val projRoot = project.toFile()

        val javaSrc = project.resolve("gateway/src/main/java/hot/sources")
        val srcFile = "NotRealClass1.java"
        val srcPath = javaSrc.resolve(srcFile)
        spliceResourceFileIntoProject("java/$srcFile", srcPath)

        // Our Java source ultimately becomes a Java class and is archived up into the JAR.
        val out1 = runTask(projRoot, "collectModlDependencies").output
        val classFile = "NotRealClass1.class"
        val subprojJar = project.resolve("gateway/build/artifacts/gateway-0.0.1-SNAPSHOT.jar").toFile()
        assertNotNull(
            ZipFile(subprojJar, OPEN_READ).getEntry("hot/sources/$classFile"),
            "Class [$classFile] not found in [$subprojJar]",
        )
        // And our target task was out-of-date.
        val outOfDatePatt = Regex(""":gateway:collectModlDependencies\R""")
        assertContains(out1, outOfDatePatt, "Expected `collectModlDependencies` task to be out-of-date")

        // ***No `clean` task between builds. We're testing the task's ability to discern dirty inputs.***
        // Now nuke the source and ensure on rebuild it is not in the resulting JAR.
        srcPath.deleteExisting()
        val out2 = runTask(projRoot, "collectModlDependencies").output
        assertNull(
            ZipFile(subprojJar, OPEN_READ).getEntry("hot/sources/$classFile"),
            "Class [$classFile] found in [$subprojJar] despite source [$srcFile] deletion prior to rebuild",
        )
        // And our target task was out-of-date.
        assertContains(out2, outOfDatePatt, "Expected `collectModlDependencies` task to be out-of-date")

        // ***Again no `clean` task between builds.***
        // Rebuild again, and ensure that `collectModlDependencies` is up to date this time.
        val out3 = runTask(projRoot, "collectModlDependencies").output
        assertContains(
            out3,
            Regex(""":gateway:collectModlDependencies UP-TO-DATE"""),
            "Expected `collectModlDependencies` task to be up-to-date"
        )
    }

    @Test
    // @Tag("IGN-6325")
    fun `subproject programmatic source file addition detected during rebuild`() {
        val projectDir = tempFolder.newFolder("sourceAddition").toPath()

        val config = GeneratorConfigBuilder()
            .moduleName("Source Addition")
            .scopes("G")
            .packageName("hot.sources")
            .parentDir(projectDir)
            .useRootForSingleScopeProject(false)
            .build()

        val project = ModuleGenerator.generate(config)
        val projRoot = project.toFile()

        // At the outset we don't have this class source in the project, and thus don't get a class in the JAR.
        val out1 = runTask(projRoot, "collectModlDependencies").output
        val srcFile = "NotRealClass1.java"
        val classFile = "NotRealClass1.class"
        val subprojJar = project.resolve("gateway/build/artifacts/gateway-0.0.1-SNAPSHOT.jar").toFile()
        assertNull(
            ZipFile(subprojJar, OPEN_READ).getEntry("hot/sources/$classFile"),
            "Class [$classFile] found in [$subprojJar] despite source [$srcFile] not yet part of the project",
        )
        // And our target task was out-of-date.
        val outOfDatePatt = Regex(""":gateway:collectModlDependencies\R""")
        assertContains(out1, outOfDatePatt, "Expected `collectModlDependencies` task to be out-of-date")

        // ***No `clean` task between builds. We're testing the task's ability to discern dirty inputs.***
        // Our new Java source ultimately becomes a Java class and is archived up into the JAR.
        val javaSrc = project.resolve("gateway/src/main/java/hot/sources")
        val srcPath = javaSrc.resolve(srcFile)
        spliceResourceFileIntoProject("java/$srcFile", srcPath)
        val out2 = runTask(projRoot, "collectModlDependencies").output
        assertNotNull(
            ZipFile(subprojJar, OPEN_READ).getEntry("hot/sources/$classFile"),
            "Class [$classFile] not found in [$subprojJar]",
        )
        // And our target task was out-of-date.
        assertContains(out2, outOfDatePatt, "Expected `collectModlDependencies` task to be out-of-date")

        // ***Again no `clean` task between builds.***
        // Rebuild again, and ensure that `collectModlDependencies` is up to date this time.
        val out3 = runTask(projRoot, "collectModlDependencies").output
        assertContains(
            out3,
            Regex(""":gateway:collectModlDependencies UP-TO-DATE"""),
            "Expected `collectModlDependencies` task to be up-to-date"
        )
    }

    @Test
    // @Tag("IGN-6325")
    fun `subproject programmatic source file mutation detected during rebuild`() {
        val projectDir = tempFolder.newFolder("sourceMutation").toPath()

        val config = GeneratorConfigBuilder()
            .moduleName("Source Mutation")
            .scopes("G")
            .packageName("hot.sources")
            .parentDir(projectDir)
            .useRootForSingleScopeProject(false)
            .build()

        val project = ModuleGenerator.generate(config)
        val projRoot = project.toFile()

        val javaSrc = project.resolve("gateway/src/main/java/hot/sources")
        val srcFile = "NotRealClass1.java"
        val srcPath = javaSrc.resolve(srcFile)
        spliceResourceFileIntoProject("java/$srcFile", srcPath)

        // Our Java source ultimately becomes a Java class and is archived up into the JAR.
        val out1 = runTask(projRoot, "collectModlDependencies").output
        val classFile = "NotRealClass1.class"
        val subprojJar = project.resolve("gateway/build/artifacts/gateway-0.0.1-SNAPSHOT.jar").toFile()
        assertNotNull(
            ZipFile(subprojJar, OPEN_READ).getEntry("hot/sources/$classFile"),
            "Class [$classFile] not found in [$subprojJar]",
        )
        // And our target task was out-of-date.
        val outOfDatePatt = Regex(""":gateway:collectModlDependencies\R""")
        assertContains(out1, outOfDatePatt, "Expected `collectModlDependencies` task to be out-of-date")

        // ***No `clean` task between builds. We're testing the task's ability to discern dirty inputs.***
        // Now tweak the source and ensure on rebuild it is still in the resulting JAR.
        val newSrcText = srcPath.readText().replaceFirst("automated", "unit")
        srcPath.writeText(newSrcText)
        val out2 = runTask(projRoot, "collectModlDependencies").output
        assertNotNull(
            ZipFile(subprojJar, OPEN_READ).getEntry("hot/sources/$classFile"),
            "Class [$classFile] disappeared from [$subprojJar]",
        )
        // And our target task was out-of-date.
        assertContains(out2, outOfDatePatt, "Expected `collectModlDependencies` task to be out-of-date")

        // ***Again no `clean` task between builds.***
        // Rebuild again, and ensure that `collectModlDependencies` is up to date this time.
        val out3 = runTask(projRoot, "collectModlDependencies").output
        assertContains(
            out3,
            Regex(""":gateway:collectModlDependencies UP-TO-DATE"""),
            "Expected `collectModlDependencies` task to be up-to-date"
        )
    }

    @Test
    // @Tag("IGN-6325")
    fun `subproject resource source file addition detected during rebuild`() {
        val projectDir = tempFolder.newFolder("resourceAddition").toPath()

        val config = GeneratorConfigBuilder()
            .moduleName("Resource Addition")
            .scopes("G")
            .packageName("hot.sources")
            .parentDir(projectDir)
            .useRootForSingleScopeProject(false)
            .build()

        val project = ModuleGenerator.generate(config)
        val projRoot = project.toFile()

        // At the outset we don't have this resource in the project, and thus don't get it in the JAR.
        val out1 = runTask(projRoot, "collectModlDependencies").output
        val resourceFile = "README.md"
        val subprojJar = project.resolve("gateway/build/artifacts/gateway-0.0.1-SNAPSHOT.jar").toFile()
        assertNull(
            ZipFile(subprojJar, OPEN_READ).getEntry(resourceFile),
            "Resource [$resourceFile] found in [$subprojJar] despite [$resourceFile] not yet part of the project",
        )
        // And our target task was out-of-date.
        val outOfDatePatt = Regex(""":gateway:collectModlDependencies\R""")
        assertContains(out1, outOfDatePatt, "Expected `collectModlDependencies` task to be out-of-date")

        // ***No `clean` task between builds. We're testing the task's ability to discern dirty inputs.***
        // Our new resource file is archived up into the JAR.
        val resources = project.resolve("gateway/src/main/resources")
        val resourcePath = resources.resolve(resourceFile)
        if (!Files.exists(resources)) { Files.createDirectories(resources) } // src/main/resources is gap in generator?
        spliceResourceFileIntoProject("certs/$resourceFile", resourcePath)
        val out2 = runTask(projRoot, "collectModlDependencies").output
        assertNotNull(
            ZipFile(subprojJar, OPEN_READ).getEntry(resourceFile),
            "Resource [$resourceFile] not found in [$subprojJar]",
        )
        // And our target task was out-of-date.
        assertContains(out2, outOfDatePatt, "Expected `collectModlDependencies` task to be out-of-date")

        // ***Again no `clean` task between builds.***
        // Rebuild again, and ensure that `collectModlDependencies` is up to date this time.
        val out3 = runTask(projRoot, "collectModlDependencies").output
        assertContains(
            out3,
            Regex(""":gateway:collectModlDependencies UP-TO-DATE"""),
            "Expected `collectModlDependencies` task to be up-to-date"
        )
    }
}
