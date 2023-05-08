package io.ia.sdk.gradle.modl.task

import com.inductiveautomation.ignitionsdk.ZipMap
import io.ia.ignition.module.generator.ModuleGenerator
import io.ia.ignition.module.generator.api.GeneratorConfigBuilder
import io.ia.ignition.module.generator.api.GradleDsl
import io.ia.sdk.gradle.modl.BaseTest
import io.ia.sdk.gradle.modl.util.signedModuleName
import org.gradle.testkit.runner.BuildResult
import org.junit.Test
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SignModuleTest : BaseTest() {
    companion object {
        const val PATH_KEY = "<FILEPATH>"
    }

    @Test
    fun `module built and signed successfully with gradle properties file`() {
        val parentDir: File = tempFolder.newFolder("module_built_and_signed_successfully")
        val moduleName = "I Was Signed"
        val signingResourcesDestination = parentDir.toPath().resolve("i-was-signed")

        prepareSigningTestResources(signingResourcesDestination)

        val config = GeneratorConfigBuilder()
            .moduleName(moduleName)
            .scopes("GCD")
            .packageName("check.my.signage")
            .parentDir(parentDir.toPath())
            .debugPluginConfig(true)
            .allowUnsignedModules(false)
            .settingsDsl(GradleDsl.GROOVY)
            .rootPluginConfig(
                """
                    id("io.ia.sdk.modl")
                """.trimIndent()
            )
            .build()

        val projectDir = ModuleGenerator.generate(config)

        val result = runTask(projectDir.toFile(), "signModule")

        val buildDir = projectDir.resolve("build")
        val signedFileName = signedModuleName(moduleName)

        val signedFilePath = "${buildDir.toAbsolutePath()}/$signedFileName"
        val signed = File(signedFilePath)

        // unzip and look for signatures file
        val zm = ZipMap(signed)
        val file = zm.get("signatures.properties")

        assertTrue(signed.exists(), "signed file exists")
        assertNotNull(file, "signatures.properties found in signed modl")
        assertTrue(result.output.toString().contains("SUCCESSFUL"))
    }

    @Test
    fun `module signed with cmdline flags`() {
        val parentDir: File = tempFolder.newFolder("module_signed_with_cmdline_flags")
        val moduleName = "I Was Signed"
        val signingResourcesDestination = parentDir.toPath().resolve("i-was-signed")

        val signResources = prepareSigningTestResources(signingResourcesDestination, false)

        val config = GeneratorConfigBuilder()
            .moduleName(moduleName)
            .scopes("GCD")
            .packageName("check.my.signage")
            .parentDir(parentDir.toPath())
            .debugPluginConfig(true)
            .allowUnsignedModules(false)
            .settingsDsl(GradleDsl.GROOVY)
            .rootPluginConfig(
                """
                    id("io.ia.sdk.modl")
                """.trimIndent()
            )
            .build()

        val projectDir = ModuleGenerator.generate(config)

        val taskArgs = listOf(
            ":signModule",
            "--keystoreFile=${signResources.keystore}",
            "--certFile=${signResources.certFile}",
            "--keystorePassword=password",
            "--certAlias=selfsigned",
            "--certPassword=password",
            "--stacktrace",
            "--info"
        )

        runTask(projectDir.toFile(), taskArgs)

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
    fun `module signing failed due to missing signing configuration properties`() {
        val name = "I Was Signed"
        val dirName = currentMethodName()
        val config = GeneratorConfigBuilder()
            .moduleName(name)
            .scopes("GCD")
            .packageName("check.my.signage")
            .parentDir(tempFolder.newFolder(dirName).toPath())
            .allowUnsignedModules(false)
            .settingsDsl(GradleDsl.GROOVY)
            .build()

        val projectDir = ModuleGenerator.generate(config)
        var result: BuildResult? = null
        var msg: String = ""
        try {
            result = runTask(projectDir.toFile(), listOf("signModule", "--certAlias=something"))
        } catch (e: Exception) {
            msg = e.message.toString()
        }

        val expected = "Some problems were found with the configuration of task ':signModule' (type 'SignModule')."

        val output: String? = result?.output
        assertNull(output, "Should have received output from build attempt")
        assertNotNull(msg, "should have exception message")
        assertTrue(msg.contains(expected), "Execution failed due to missing sign props")
    }

    @Test
    fun `module failed with missing keystore pw flags`() {
        val dirName = currentMethodName()
        val moduleName = "I Was Signed"
        val workingDir: File = tempFolder.newFolder(dirName)

        val config = GeneratorConfigBuilder()
            .moduleName(moduleName)
            .scopes("GCD")
            .packageName("check.my.signage")
            .parentDir(workingDir.toPath())
            .debugPluginConfig(true)
            .allowUnsignedModules(false)
            .settingsDsl(GradleDsl.GROOVY)
            .rootPluginConfig(
                """
                    id("io.ia.sdk.modl")
                """.trimIndent()
            )
            .build()

        val projectDir = ModuleGenerator.generate(config)
        val signResources = prepareSigningTestResources(projectDir, false)

        val taskArgs = listOf(
            ":signModule",
            "--keystoreFile=${signResources.keystore}",
            "--certFile=${signResources.certFile}",
            "--certAlias=selfsigned",
            "--certPassword=password"
        )
        var result: BuildResult? = null
        var ex: Exception? = null
        try {
            result = runTask(projectDir.toFile(), taskArgs)
        } catch (e: Exception) {
            ex = e
        }

        val expectedError = Regex(
            """> Task :signModule FAILED\RRequired keystore password not found.  Specify via flag """ +
            "'--keystorePassword=<value>', or in gradle.properties file as 'ignition.signing.keystorePassword=<value>'"
        )
        assertNull(result, "build output will be null due to failure")
        assertNotNull(ex, "Exception should be caught and not null")
        assertNotNull(ex.message, "Exception should have message")
        assertTrue(ex.message.toString().contains(expectedError), "expected error detected.")
    }

    @Test
    fun `module failed with missing cert pw flags`() {
        val dirName = currentMethodName()
        val moduleName = "I Was Signed"
        val workingDir: File = tempFolder.newFolder(dirName)

        val config = GeneratorConfigBuilder()
            .moduleName(moduleName)
            .scopes("GCD")
            .packageName("check.my.signage")
            .parentDir(workingDir.toPath())
            .debugPluginConfig(true)
            .allowUnsignedModules(false)
            .settingsDsl(GradleDsl.GROOVY)
            .rootPluginConfig(
                """
                    id("io.ia.sdk.modl")
                """.trimIndent()
            )
            .build()

        val projectDir = ModuleGenerator.generate(config)
        val signResources = prepareSigningTestResources(projectDir, false)

        val taskArgs = listOf(
            ":signModule",
            "--keystoreFile=${signResources.keystore}",
            "--certFile=${signResources.certFile}",
            "--certAlias=selfsigned",
            "--certPassword=password"
        )
        var result: BuildResult? = null
        var ex: Exception? = null
        try {
            result = runTask(projectDir.toFile(), taskArgs)
        } catch (e: Exception) {
            ex = e
        }

        val expectedError = Regex(
            """> Task :signModule FAILED\RRequired keystore password not found.  Specify via flag """ +
            "'--keystorePassword=<value>', or in gradle.properties file as 'ignition.signing.keystorePassword=<value>'"
        )
        assertNull(result, "build output will be null due to failure")
        assertNotNull(ex, "Exception should be caught and not null")
        assertNotNull(ex.message, "Exception should have message")
        assertTrue(ex.message.toString().contains(expectedError), "expected error detected.")
    }
}
