package io.ia.sdk.gradle.modl

import io.ia.ignition.module.generator.api.GeneratorConfig
import io.ia.ignition.module.generator.api.GeneratorConfigBuilder
import io.ia.sdk.gradle.modl.api.Constants
import io.ia.sdk.gradle.modl.api.Constants.ALIAS_FLAG
import io.ia.sdk.gradle.modl.api.Constants.CERT_FILE_FLAG
import io.ia.sdk.gradle.modl.api.Constants.CERT_PW_FLAG
import io.ia.sdk.gradle.modl.api.Constants.KEYSTORE_FILE_FLAG
import io.ia.sdk.gradle.modl.api.Constants.KEYSTORE_PW_FLAG
import io.ia.sdk.gradle.modl.util.nameToDirName
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

data class SigningResources(
    val keystore: Path,
    val certFile: Path,
    val signPropFile: Path?
)

open class BaseTest {
    companion object {
        val CLIENT_DEP = "// add client scoped dependencies here"
        val DESIGNER_DEP = "// add designer scoped dependencies here"
        val GW_DEP = "// add gateway scoped dependencies here"
        val COMMON_DEP = "// add common scoped dependencies here"
        val SIGN_PROPS = "signing.properties"
        val SIGNING_PROPERTY_ENTRIES = """
            ${Constants.SIGNING_PROPERTIES[ALIAS_FLAG]}=selfsigned
            ${Constants.SIGNING_PROPERTIES[KEYSTORE_FILE_FLAG]}=./keystore.jks
            ${Constants.SIGNING_PROPERTIES[KEYSTORE_PW_FLAG]}=password
            ${Constants.SIGNING_PROPERTIES[CERT_FILE_FLAG]}=./certificate.pem
            ${Constants.SIGNING_PROPERTIES[CERT_PW_FLAG]}=password
        """.trimIndent()
    }

    @Rule @JvmField
    val tempFolder = TemporaryFolder()

    /**
     * Returns the name of the method/function that calls this.
     */
    protected fun currentMethodName(): String {
        val name = Thread.currentThread().stackTrace[2].methodName.replace(" ", "")
        return name
    }

    protected fun prepSigningResourcesForModuleName(
        parentDirectory: Path,
        moduleName: String,
        withPropFile: Boolean = true
    ): SigningResources {
        val projectDir = nameToDirName(moduleName)
        return prepareSigningTestResources(parentDirectory.resolve(projectDir), withPropFile)
    }

    // writes a gradle.properties file containing the signing credentials needed for signing a module using test
    // resources
    protected fun writeSigningCredentials(targetDirectory: Path): Path {
        val gradleProps: Path = targetDirectory.resolve("gradle.properties")

        if (Files.exists(gradleProps)) {
            val content = gradleProps.toFile().readText(Charsets.UTF_8)
            gradleProps.toFile().writeText(content + "\n" + SIGNING_PROPERTY_ENTRIES)
        } else {
            gradleProps.toFile().writeText(SIGNING_PROPERTY_ENTRIES, Charsets.UTF_8)
        }

        return gradleProps
    }

    fun moduleDirName(moduleName: String): String {
        return moduleName.replace(" ", "-").toLowerCase()
    }

    // returns the path to the 'signing.properties' file
    protected fun prepareSigningTestResources(targetDirectory: Path, withPropFile: Boolean = true): SigningResources {
        Files.createDirectories(targetDirectory)

        val paths: List<Path?> = listOf("certificate.pem", "keystore.jks").map { resourcePath ->
            ClassLoader.getSystemResourceAsStream("certs/$resourcePath").let { inputStream ->
                if (inputStream == null) {
                    throw Exception("Failed to read test resource 'certs/$resourcePath")
                }
                val writeTo = targetDirectory.resolve(resourcePath)
                inputStream.copyTo(writeTo.toFile().outputStream(), 1024)

                writeTo
            }
        }

        return SigningResources(
            certFile = paths[0] as Path,
            keystore = paths[1] as Path,
            signPropFile = if (withPropFile) writeSigningCredentials(targetDirectory) else null
        )
    }

    open fun config(name: String, scope: String, pkg: String): GeneratorConfig {
        val testDir = listOf(
            name.replace(" ", "_"),
            scope,
            pkg.replace(".", "_")
        ).joinToString("")

        return GeneratorConfigBuilder()
            .moduleName(name)
            .scopes(scope)
            .packageName(pkg)
            .parentDir(tempFolder.newFolder(testDir).toPath())
            .build()
    }

    open fun runTask(projectDir: File, taskArgs: List<String>): BuildResult {
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withDebug(true)
        runner.withPluginClasspath()
        runner.withArguments(taskArgs)
        runner.withProjectDir(projectDir)
        return runner.build()
    }

    open fun runTask(projectDir: File, task: String): BuildResult {
        return runTask(projectDir, listOf(task))
    }
}
