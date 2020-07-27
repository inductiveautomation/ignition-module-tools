package io.ia.sdk.gradle.modl

import io.ia.ignition.module.generator.api.GeneratorConfig
import io.ia.ignition.module.generator.api.GeneratorConfigBuilder
import io.ia.sdk.gradle.modl.util.nameToDirName
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

open class BaseTest {
    companion object {
        val CLIENT_DEP = "// add client scoped dependencies here"
        val DESIGNER_DEP = "// add designer scoped dependencies here"
        val GW_DEP = "// add gateway scoped dependencies here"
        val COMMON_DEP = "// add common scoped dependencies here"
        val SIGN_PROPS = "signing.properties"
    }

    @get:Rule
    val tempFolder = TemporaryFolder()

    protected fun prepSigningResourcesForModuleName(parentDirectory: Path, moduleName: String): Path {
        val projectDir = nameToDirName(moduleName)
        return prepareSigningTestResources(parentDirectory.resolve(projectDir))
    }

    // returns the path to the 'signing.properties' file
    protected fun prepareSigningTestResources(targetDirectory: Path): Path {
        Files.createDirectories(targetDirectory)

        var signProps: Path? = null
        listOf(SIGN_PROPS, "certificate.pem", "keystore.jks").forEach { resourcePath ->
            ClassLoader.getSystemResourceAsStream("certs/$resourcePath").use { inputStream ->
                if (inputStream != null) {
                    val targetFile = targetDirectory.resolve(resourcePath)
                    inputStream.copyTo(targetFile.toFile().outputStream(), 1024)
                    if (targetFile.toFile().name == SIGN_PROPS) {
                        signProps = targetFile
                    }
                } else {
                    throw Exception("Failed to read test resource 'certs/$resourcePath")
                }
            }
        }

        if (signProps == null) {
            throw Exception("Signing property file cannot be null!")
        }

        return signProps as Path
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

    open fun runTask(projectDir: File, taskName: String): BuildResult {
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments(taskName)
        runner.withProjectDir(projectDir)
        return runner.build()
    }
}
