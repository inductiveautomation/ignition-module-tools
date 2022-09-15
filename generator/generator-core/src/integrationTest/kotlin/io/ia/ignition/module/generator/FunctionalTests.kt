package io.ia.ignition.module.generator

import io.ia.ignition.module.generator.api.GeneratorConfigBuilder
import io.ia.ignition.module.generator.api.GradleDsl
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

class FunctionalTests {
    @get:Rule
    val tempFolder = TemporaryFolder()

    data class TestConfig(val moduleName: String, val packageName: String, val scope: String, val dir: Path)

    private fun dir(folderName: String): Path {
        return tempFolder.newFolder(folderName).toPath()
    }

    private enum class OS {
        NIXLIKE, WIN;
    }

    private fun os(): OS {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> OS.WIN
            os.contains("nix") || os.contains("nux") || os.contains("aix") || os.contains("mac") -> {
                OS.NIXLIKE
            }

            else -> throw Exception("Could not resolve platform from system property 'os.name' with value: $os")
        }
    }

    fun command(taskConfig: String): Set<String> {
        return when (os()) {
            OS.WIN -> setOf("cmd.exe", "/c", "gradlew.bat $taskConfig")
            OS.NIXLIKE -> setOf("sh", "-c", "./gradlew $taskConfig")
        }
    }

    private fun applyExecPermissions(file: Path) {
        val perms: MutableSet<PosixFilePermission> = Files.readAttributes(
            file,
            PosixFileAttributes::class.java
        ).permissions()

        perms.add(PosixFilePermission.OWNER_WRITE)
        perms.add(PosixFilePermission.OWNER_READ)
        perms.add(PosixFilePermission.OWNER_EXECUTE)
        perms.add(PosixFilePermission.GROUP_WRITE)
        perms.add(PosixFilePermission.GROUP_READ)
        perms.add(PosixFilePermission.GROUP_EXECUTE)
        perms.add(PosixFilePermission.OTHERS_WRITE)
        perms.add(PosixFilePermission.OTHERS_READ)
        perms.add(PosixFilePermission.OTHERS_EXECUTE)

        Files.setPosixFilePermissions(file, perms)
    }

    private fun String.runCommand(workingDir: Path): String {
        if (os() == OS.NIXLIKE) {
            // set group/all user permissions so script can exec
            applyExecPermissions(workingDir.resolve("gradlew"))
        }
        val cmd = command(this)
        val process = ProcessBuilder(*(cmd.toTypedArray()))
            .directory(workingDir.toFile())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        val result = process.inputStream.bufferedReader().readText()

        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroy()
            throw RuntimeException("execution timed out: $this")
        }
        if (process.exitValue() != 0) {
            throw RuntimeException("execution failed with code ${process.exitValue()}: $this")
        }
        return result
    }

    @Test
    fun `generated groovy buildscript projects build successfully`() {
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
                .buildscriptDsl(GradleDsl.GROOVY)
                .build()

            val projectRootDir: Path = ModuleGenerator.generate(config)

            var processOutput = "build".runCommand(projectRootDir)
            assertTrue(processOutput.contains("BUILD SUCCESSFUL"))
        }
    }

    @Test
    fun `generated kotlin buildscript projects build successfully`() {
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
                .buildscriptDsl(GradleDsl.KOTLIN)
                .build()

            val projectRootDir: Path = ModuleGenerator.generate(config)

            val processOutput = "build".runCommand(projectRootDir)
            assertTrue(processOutput.contains("BUILD SUCCESSFUL"))
        }
    }
}
