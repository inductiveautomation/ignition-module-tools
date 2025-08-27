package io.ia.sdk.gradle.modl.task

import io.ia.ignition.module.generator.ModuleGenerator
import io.ia.sdk.gradle.modl.BaseTest
import io.ia.sdk.gradle.modl.util.unsignedModuleName
import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ZipModuleTests : BaseTest() {

    private lateinit var tempDir: File
    private lateinit var project: Project
    private lateinit var task: ZipModule

    @BeforeTest
    fun setup() {
        tempDir = createTempDirectory().toFile()
        project = ProjectBuilder.builder().withProjectDir(tempDir).build() as DefaultProject
        task = project.tasks.create("testTask", ZipModule::class.java)

        // Set up the task properties with the temp directory
        task.content.set(project.objects.directoryProperty().fileValue(File(tempDir, "content")))
        task.unsignedModule.set(project.objects.fileProperty().fileValue(File(tempDir, "output.modl")))
    }

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `task succeeds when no duplicate jars exist`() {
        // Arrange
        val contentDir = task.content.asFile.get().apply { mkdirs() }
        File(contentDir, "my-lib-1.0.jar").createNewFile()
        File(contentDir, "another-lib-2.0.jar").createNewFile()

        // Act: The task should not throw an exception
        task.execute()
    }

    @Test
    fun `task fails when duplicate jars with different versions exist`() {
        // Arrange
        val contentDir = task.content.asFile.get().apply { mkdirs() }
        File(contentDir, "my-lib-1.0.jar").createNewFile()
        File(contentDir, "my-lib-2.0.jar").createNewFile() // This is the duplicate

        // Act & Assert: The task should throw a IllegalArgumentException
        val exception = assertFailsWith<IllegalArgumentException> {
            task.execute()
        }

        // Verify the exception message
        val expectedMessage = "Library 'my-lib' exists in multiple versions"
        assertTrue(exception.message!!.contains(expectedMessage))
    }

    @Test
    fun `unsigned module is built and has appropriate name`() {
        val name = "Some Thing"
        val config = config(name, "GC", "net.some.thing")
        val projectDir = ModuleGenerator.generate(config)
        runTask(projectDir.toFile(), "zipModule")

        val buildDir = projectDir.resolve("build")
        val zipFileName = unsignedModuleName(name)

        assertTrue(buildDir.toFile().exists())
        assertTrue(buildDir.resolve(zipFileName).toFile().exists(), "Unsigned module created with correct name")
    }

    fun `unzipping unsigned module reveals correct contents`() {
        val name = "Unzip Me"

        val config = config(name, "G", "unpack.the.pack")
        val projectDir = ModuleGenerator.generate(config)
        runTask(projectDir.toFile(), "zipModule")

        val buildDir = projectDir.resolve("build")
        val zipFileName = unsignedModuleName(name)
        assertTrue(buildDir.toFile().exists())
        assertTrue(buildDir.resolve(zipFileName).toFile().exists())
    }
}
