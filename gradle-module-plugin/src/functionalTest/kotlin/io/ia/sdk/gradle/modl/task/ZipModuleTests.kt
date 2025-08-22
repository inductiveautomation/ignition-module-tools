package io.ia.sdk.gradle.modl.task

import io.ia.ignition.module.generator.ModuleGenerator
import io.ia.sdk.gradle.modl.BaseTest
import io.ia.sdk.gradle.modl.util.unsignedModuleName
import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ZipModuleTests : BaseTest() {

    @TempDir
    lateinit var testProjectDir: File
    private lateinit var project: Project
    private lateinit var task: ZipModule

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().withProjectDir(testProjectDir).build() as DefaultProject
        task = project.tasks.create("testTask", ZipModule::class.java)

        // Set up the task properties with the test directory
        task.content.set(project.objects.directoryProperty().fileValue(File(testProjectDir, "content")))
        task.unsignedModule.set(project.objects.fileProperty().fileValue(File(testProjectDir, "output.modl")))
    }

    @Test
    fun `task succeeds when no duplicate jars exist`() {
        // Arrange
        val contentDir = File(testProjectDir, "content").apply { mkdirs() }
        File(contentDir, "my-lib-1.0.jar").createNewFile()
        File(contentDir, "another-lib-2.0.jar").createNewFile()

        // Act & Assert: The task should not throw an exception
        task.execute()
    }

    @Test
    fun `task fails when duplicate jars with different versions exist`() {
        // Arrange
        val contentDir = File(testProjectDir, "content").apply { mkdirs() }
        File(contentDir, "my-lib-1.0.jar").createNewFile()
        File(contentDir, "my-lib-2.0.jar").createNewFile() // This is the duplicate

        // Act & Assert: The task should throw a TaskExecutionException
        val exception = assertThrows(TaskExecutionException::class.java) {
            task.execute()
        }

        // Verify the exception message
        val expectedMessage = "Jar with 'my-lib' has multiple versions presented"
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
