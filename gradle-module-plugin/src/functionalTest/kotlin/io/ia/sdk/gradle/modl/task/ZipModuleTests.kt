package io.ia.sdk.gradle.modl.task

import io.ia.ignition.module.generator.ModuleGenerator
import io.ia.sdk.gradle.modl.BaseTest
import io.ia.sdk.gradle.modl.util.unsignedModuleName
import kotlin.test.assertTrue
import org.junit.Test

class ZipModuleTests : BaseTest() {

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
