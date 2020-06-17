package io.ia.sdk.gradle.modl.task

import com.inductiveautomation.ignitionsdk.ZipMap
import io.ia.ignition.module.generator.ModuleGenerator
import io.ia.ignition.module.generator.api.GeneratorConfig
import io.ia.sdk.gradle.modl.BaseTest
import io.ia.sdk.gradle.modl.util.signedModuleName
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.testkit.runner.BuildResult
import org.junit.Test

class SignModuleTest : BaseTest() {
    companion object {
        const val PATH_KEY = "<FILEPATH>"
        const val PROP_FILE_PATH_FORMAT = "project.file(\"$PATH_KEY\")"
    }

    @Test
    fun `module built and signed successfully`() {
        val parentDir: File = tempFolder.newFolder("module_built_and_signed_successfully")
        val moduleName = "I Was Signed"
        val signingResourcesDestination = parentDir.toPath().resolve("i-was-signed")
        val testSigningPropertyFilePath = signingResourcesDestination.resolve("signing.properties")

        prepareSigningTestResources(signingResourcesDestination)

        val config = GeneratorConfig.ConfigBuilder()
            .moduleName(moduleName)
            .scopes("GCD")
            .packageName("check.my.signage")
            .parentDir(parentDir.toPath())
            .signingCredentialPropertyFile(
                PROP_FILE_PATH_FORMAT.replace(PATH_KEY, testSigningPropertyFilePath.toString())
            )
            .debugPluginConfig(true)
            .rootPluginConfig("""
                    id("io.ia.sdk.modl") version("0.0.1-beta1")
                """.trimIndent())
            .build()

        val projectDir = ModuleGenerator.generate(config)

        runTask(projectDir.toFile(), "signModule")

        val buildDir = projectDir.resolve("build")
        val signedFileName = signedModuleName(moduleName)

        val signedFilePath = "${buildDir.toAbsolutePath()}/$signedFileName"
        val signed = File(signedFilePath)

        // unzip and look for signatures file
        val zm = ZipMap(signed)
        val file = zm.get("signatures.properties")

        assertTrue(signed.exists(), "signed file exists")
        assertNotNull(file, "signatures.properties found in signed modl")
    }

    @Test
    fun `module signing failed due to missing property file`() {
        val name = "I Was Signed"
        val propFile = PROP_FILE_PATH_FORMAT.replace(PATH_KEY, """/some/bunk/path/signing.properties""")
        val config = GeneratorConfig.ConfigBuilder()
            .moduleName(name)
            .scopes("GCD")
            .packageName("check.my.signage")
            .parentDir(tempFolder.newFolder("signing_failed_due_to_missing_prop_file").toPath())
            .signingCredentialPropertyFile(propFile)
            .build()

        val projectDir = ModuleGenerator.generate(config)
        var result: BuildResult? = null
        var msg: String = ""
        try {
            result = runTask(projectDir.toFile(), "signModule")
        } catch (e: Exception) {
            msg = e.message.toString()
        }

        val expected =
            "A problem was found with the configuration of task ':signModule' (type 'SignModule').\n" +
                "> File '/some/bunk/path/signing.properties' specified for property 'propertyFilePath' does not exist."

        val output: String? = result?.output
        assertNull(output, "Should have received output from build attempt")
        assertNotNull(msg, "should have exception message")
        assertTrue(msg.contains(expected), "Execution failed due to missing prop file")
    }
}
