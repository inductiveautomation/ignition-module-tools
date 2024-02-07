package io.ia.sdk.gradle.modl.task

import com.inductiveautomation.ignitionsdk.ZipMap
import com.inductiveautomation.ignitionsdk.ZipMapFile
import io.ia.ignition.module.generator.ModuleGenerator
import io.ia.ignition.module.generator.api.GeneratorConfigBuilder
import io.ia.ignition.module.generator.api.GradleDsl
import io.ia.sdk.gradle.modl.BaseTest
import io.ia.sdk.gradle.modl.util.signedModuleName
import io.ia.sdk.gradle.modl.util.unsignedModuleName
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import java.io.File
import java.nio.file.Path
import kotlin.test.Ignore
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SignModuleTest : BaseTest() {
    companion object {
        const val PATH_KEY = "<FILEPATH>"
        const val MODULE_NAME = "I Was Signed"
        // For a specific YubiKey 5; you may need to change this for another key
        val PKCS11_HSM_SIGNING_PROPERTY_ENTRIES = """
            # Hack around YK5 (signing) slot 9c's second PIN challenge on
            # signing following the initial keystore PIN challenge by using a
            # cert in (auth) slot 9a.
            #
            #ignition.signing.certAlias=X.509 Certificate for Digital Signature
            ignition.signing.certAlias=X.509 Certificate for PIV Authentication
            ignition.signing.keystorePassword=123456
            ignition.signing.certFile=./pkcs11-yk5-win.crt
            #
            # Ignored for now, but may be used in future for slot 9c.
            ignition.signing.certPassword=password
            ignition.signing.pkcs11CfgFile=./pkcs11-yk5-win.cfg
        """.trimIndent()

        const val SIG_PROPERTIES_FILENAME = "signatures.properties"
        const val CERT_PKCS7_FILENAME = "certificates.p7b"
    }

    @Test
    fun `module built and signed successfully with gradle properties file`() {
        val parentDir: File = tempFolder.newFolder("module_built_and_signed_successfully")
        val signingResourcesDestination = parentDir.toPath().resolve("i-was-signed")

        prepareSigningTestResources(signingResourcesDestination)

        val projectDir = generateModule(parentDir)

        runTask(
            projectDir.toFile(), listOf("signModule", "--stacktrace")
        )

        val buildDir = projectDir.resolve("build")
        val signedFileName = signedModuleName(MODULE_NAME)

        val signedFilePath = "${buildDir.toAbsolutePath()}/$signedFileName"
        val signed = File(signedFilePath)

        // unzip and look for signatures properties file
        val zm = ZipMap(signed)
        val sigPropsFile = zm[SIG_PROPERTIES_FILENAME]
        assertTrue(signed.exists(), "Expected $signed to exist")
        assertNotNull(
            sigPropsFile,
            "Expected $SIG_PROPERTIES_FILENAME in signed modl"
        )

        // and the cert file
        val certFile = zm[CERT_PKCS7_FILENAME]
        assertNotNull(
            certFile,
            "Expected $CERT_PKCS7_FILENAME in signed modl"
        )

        // If you want to dump file contents to stdout, uncomment this
        // logZipMapFileText(SIG_PROPERTIES_FILENAME, sigPropsFile)
        // logZipMapFileText(CERT_PKCS7_FILENAME, certFile)
    }

    @Test
    fun `module signed with cmdline flags`() {
        val parentDir: File = tempFolder.newFolder("module_signed_with_cmdline_flags")
        val signingResourcesDestination = parentDir.toPath().resolve("i-was-signed")

        val signResources = prepareSigningTestResources(
            signingResourcesDestination,
            withPropFile = false,
        )

        val projectDir = generateModule(parentDir)

        val taskArgs = listOf(
            ":signModule",
            "--keystoreFile=${signResources.keystore}",
            "--certFile=${signResources.certFile}",
            "--keystorePassword=password",
            "--certAlias=selfsigned",
            "--certPassword=password",
            "--stacktrace",
        )

        runTask(projectDir.toFile(), taskArgs)

        val buildDir = projectDir.resolve("build")
        val signedFileName = signedModuleName(MODULE_NAME)

        val signedFilePath = "${buildDir.toAbsolutePath()}/$signedFileName"
        val signed = File(signedFilePath)

        // unzip and look for signatures properties file
        val zm = ZipMap(signed)
        val sigPropsFile = zm[SIG_PROPERTIES_FILENAME]
        assertTrue(signed.exists(), "Expected $signed to exist")
        assertNotNull(
            sigPropsFile,
            "Expected $SIG_PROPERTIES_FILENAME in signed modl"
        )

        // and the cert file
        val certFile = zm[CERT_PKCS7_FILENAME]
        assertNotNull(
            certFile,
            "Expected $CERT_PKCS7_FILENAME in signed modl"
        )

        // If you want to dump file contents to stdout, uncomment this
        // logZipMapFileText(SIG_PROPERTIES_FILENAME, sigPropsFile)
        // logZipMapFileText(CERT_PKCS7_FILENAME, certFile)
    }

    @Test
    fun `module signing failed due to missing signing configuration properties`() {
        val dirName = currentMethodName()

        val projectDir = generateModule(tempFolder.newFolder(dirName))

        val result: BuildResult =
            runTaskAndFail(
                projectDir.toFile(),
                listOf("signModule", "--certAlias=something")
            )

        val out = result.output
        assertNotNull(out, "Expected exception with message")

        assertContains(out, "Required certificate file location not found")
        assertContains(out, "Specify via flag '--certFile=<value>'")
        assertContains(
            out,
            "file as 'ignition.signing.certFile=<value>"
        )

        assertContains(out, "Required certificate password not found")
        assertContains(out, "Specify via flag '--certPassword=<value>'")
        assertContains(
            out,
            "file as 'ignition.signing.certPassword=<value>"
        )

        assertContains(out, "Required keystore password not found")
        assertContains(out, "Specify via flag '--keystorePassword=<value>'")
        assertContains(
            out,
            "file as 'ignition.signing.keystorePassword=<value>"
        )
    }

    @Test
    fun `module failed with missing keystore pw flags`() {
        val dirName = currentMethodName()
        val workingDir: File = tempFolder.newFolder(dirName)

        val projectDir = generateModule(workingDir)

        val signResources = prepareSigningTestResources(
            projectDir,
            withPropFile = false,
        )

        val taskArgs = listOf(
            ":signModule",
            "--keystoreFile=${signResources.keystore}",
            "--certFile=${signResources.certFile}",
            "--certAlias=selfsigned",
            "--certPassword=password",
            "--stacktrace",
        )

        val result: BuildResult =
            runTaskAndFail(
                projectDir.toFile(),
                taskArgs
            )

        val out = result.output
        val expectedError = Regex(
            """> Task :signModule FAILED\RRequired keystore password not found.  Specify via flag """ +
                "'--keystorePassword=<value>', or in gradle.properties file as 'ignition.signing.keystorePassword=<value>'"
        )
        assertContains(out, expectedError)
    }

    @Test
    fun `module failed with missing cert pw flags`() {
        val dirName = currentMethodName()
        val workingDir: File = tempFolder.newFolder(dirName)

        val projectDir = generateModule(workingDir)

        val signResources = prepareSigningTestResources(
            projectDir,
            withPropFile = false,
        )

        val taskArgs = listOf(
            ":signModule",
            "--keystoreFile=${signResources.keystore}",
            "--certFile=${signResources.certFile}",
            "--certAlias=selfsigned",
            "--keystorePassword=password",
            "--stacktrace",
        )

        val result: BuildResult =
            runTaskAndFail(
                projectDir.toFile(),
                taskArgs
            )

        val out = result.output
        val expectedError = Regex(
            """> Task :signModule FAILED\RRequired certificate password not found.  Specify via flag """ +
                "'--certPassword=<value>', or in gradle.properties file as 'ignition.signing.certPassword=<value>'"
        )
        assertContains(out, expectedError)
    }

    @Test
    // @Tag("IGN-7871")
    fun `module failed - file and pkcs11 keystore in gradle properties`() {
        val dirName = currentMethodName()
        val workingDir: File = tempFolder.newFolder(dirName)

        val projectDir = generateModule(workingDir)

        // These calls yield a gradle.properties file with both file- and
        // PKCS#11-based keystore config--a conflict.
        val signingResourcesDestination =
            workingDir.toPath().resolve("i-was-signed")
        writeResourceFiles(
            signingResourcesDestination,
            listOf("certificate.pem", "keystore.jks", "pkcs11.cfg")
        )
        writeSigningCredentials(
            signingResourcesDestination,
            "$PKCS11_PROPERTY_ENTRIES\n$KEYSTORE_PROPERTY_ENTRIES"
        )

        val result: BuildResult =
            runTaskAndFail(
                projectDir.toFile(),
                listOf("signModule", "--stacktrace")
            )

        val task = result.task(":signModule")
        assertEquals(task?.outcome, TaskOutcome.FAILED)
        assertContains(
            result.output,
            "'--keystoreFile' flag/'ignition.signing.keystoreFile' property " +
                "in gradle.properties or " +
                "'--pkcs11CfgFile' flag/'ignition.signing.pkcs11CfgFile' property " +
                "in gradle.properties but not both"
        )
        assertContains(result.output, "InvalidUserDataException")
    }

    @Test
    // @Tag("IGN-7871")
    fun `module failed - file keystore in gradle properties, pkcs11 keystore on cmdline`() {
        val dirName = currentMethodName()
        val workingDir: File = tempFolder.newFolder(dirName)

        val projectDir = generateModule(workingDir)

        // Write file-based keystore + specify in gradle.properties.
        val signingResourcesDestination =
            workingDir.toPath().resolve("i-was-signed")
        prepareSigningTestResources(signingResourcesDestination)

        // Also write PKCS#11 HSM config, which by itself is OK.
        val pkcs11CfgPath = writeResourceFiles(
            signingResourcesDestination, listOf("pkcs11.cfg")
        ).first()

        // But specifying that file via option suggests there is an HSM
        // keystore, which conflicts with the file-based keystore.
        val taskArgs = listOf(
            "signModule",
            "--pkcs11CfgFile=$pkcs11CfgPath",
            "--stacktrace",
        )
        val result: BuildResult =
            runTaskAndFail(projectDir.toFile(), taskArgs)

        val task = result.task(":signModule")
        assertEquals(task?.outcome, TaskOutcome.FAILED)
        assertContains(
            result.output,
            "'--keystoreFile' flag/'ignition.signing.keystoreFile' property " +
                "in gradle.properties or " +
                "'--pkcs11CfgFile' flag/'ignition.signing.pkcs11CfgFile' property " +
                "in gradle.properties but not both"
        )
        assertContains(result.output, "InvalidUserDataException")
    }

    @Test
    // @Tag("IGN-7871")
    fun `module failed - file keystore on cmdline, pkcs11 keystore in gradle properties`() {
        val dirName = currentMethodName()
        val workingDir: File = tempFolder.newFolder(dirName)

        val projectDir = generateModule(workingDir)

        // Write PKCS#11 HSM config file + specify in gradle.properties.
        val signingResourcesDestination =
            workingDir.toPath().resolve("i-was-signed")
        preparePKCS11SigningTestResources(signingResourcesDestination)

        // Also write file-based keystore, which by itself is OK.
        val ksPath = writeResourceFiles(
            signingResourcesDestination, listOf("keystore.jks")
        ).first()

        // But specifying that file suggests there is a file keystore,
        // which conflicts with the HSM keystore.
        val taskArgs = listOf(
            "signModule",
            "--keystoreFile=$ksPath",
            "--stacktrace",
        )
        val result: BuildResult =
            runTaskAndFail(projectDir.toFile(), taskArgs)

        val task = result.task(":signModule")
        assertEquals(task?.outcome, TaskOutcome.FAILED)
        assertContains(
            result.output,
            "'--keystoreFile' flag/'ignition.signing.keystoreFile' property " +
                "in gradle.properties or " +
                "'--pkcs11CfgFile' flag/'ignition.signing.pkcs11CfgFile' property " +
                "in gradle.properties but not both"
        )
        assertContains(result.output, "InvalidUserDataException")
    }

    @Test
    // @Tag("IGN-7871")
    fun `skip signing - no need for signing properties`() {
        val dirName = currentMethodName()
        val workingDir: File = tempFolder.newFolder(dirName)

        val projectDir = generateModule(
            workingDir,
            skipSigning = true, // sets project extension prop > task prop
        )

        // We've written no signing properties nor passed any via task args
        val result = runTask(projectDir.toFile(), ":signModule")

        val task = result.task(":signModule")
        assertEquals(task?.outcome, TaskOutcome.SKIPPED)
        assertContains(result.output!!, "Module Signing will be skipped")

        val buildDir = projectDir.resolve("build")
        val unsignedFileName = unsignedModuleName(MODULE_NAME)

        val unsigned = File("${buildDir.toAbsolutePath()}/$unsignedFileName")
        assertTrue(unsigned.exists(), "Expected $unsigned to exist")
    }

    @Test
    // @Tag("IGN-7871")
    fun `module failed - file and pkcs11 keystore on cmdline`() {
        val dirName = currentMethodName()
        val workingDir: File = tempFolder.newFolder(dirName)

        val projectDir = generateModule(workingDir)

        // Write PKCS#11 HSM config file + write file-based keystore, which
        // by itself is OK.
        val signingResourcesDestination =
            workingDir.toPath().resolve("i-was-signed")
        val (ksPath, pkcs11Cfg) = writeResourceFiles(
            signingResourcesDestination,
            listOf("keystore.jks", "pkcs11.cfg", "certificate.pem")
        )

        // But specifying that file suggests there is a file keystore,
        // which conflicts with the HSM keystore.
        val taskArgs = listOf(
            "signModule",
            "--keystoreFile=$ksPath",
            "--pkcs11CfgFile=$pkcs11Cfg",
            "--keystorePassword=password",
            "--certAlias=selfsigned",
            "--certFile=./certificate.pem",
            "--certPassword=password",
            "--stacktrace",
        )
        val result: BuildResult =
            runTaskAndFail(projectDir.toFile(), taskArgs)

        val task = result.task(":signModule")
        assertEquals(task?.outcome, TaskOutcome.FAILED)
        assertContains(
            result.output,
            "'--keystoreFile' flag/'ignition.signing.keystoreFile' property " +
                "in gradle.properties or " +
                "'--pkcs11CfgFile' flag/'ignition.signing.pkcs11CfgFile' property " +
                "in gradle.properties but not both"
        )
        assertContains(result.output, "InvalidUserDataException")
    }

    // This is a test with an actual PKCS#11-compliant YubiKey 5, on Windows.
    // As such it is typically set to @Ignore.
    @Test
    @Ignore
    // @Tag("integration") // break out into a test suite at some point
    // @Tag("IGN-7871")
    fun `integration - module signed with physical pkcs11 HSM in gradle properties`() {
        val dirName = currentMethodName()
        val workingDir: File = tempFolder.newFolder(dirName)

        val projectDir = generateModule(workingDir)

        // Write PKCS#11 config file and and cert file, and specify them in
        // gradle.properties.
        val signingResourcesDestination =
            workingDir.toPath().resolve("i-was-signed")
        writeResourceFiles(
            signingResourcesDestination,
            listOf("pkcs11-yk5-win.crt", "pkcs11-yk5-win.cfg")
        )
        writeSigningCredentials(
            targetDirectory = signingResourcesDestination,
            keystoreProps = PKCS11_HSM_SIGNING_PROPERTY_ENTRIES,
            writeBoilerplateProps = false,
        )

        val result: BuildResult = runTask(
            projectDir.toFile(),
            listOf("signModule", "--stacktrace")
        )

        val task = result.task(":signModule")
        assertEquals(task?.outcome, TaskOutcome.SUCCESS)

        val buildDir = projectDir.resolve("build")
        val signedFileName = signedModuleName(MODULE_NAME)

        val signed = File("${buildDir.toAbsolutePath()}/$signedFileName")

        // unzip and look for signatures properties file
        val zm = ZipMap(signed)
        val sigPropsFile = zm[SIG_PROPERTIES_FILENAME]
        assertTrue(signed.exists(), "Expected $signed to exist")
        assertNotNull(
            sigPropsFile,
            "Expected $SIG_PROPERTIES_FILENAME in signed modl"
        )

        // and the cert file
        val certFile = zm[CERT_PKCS7_FILENAME]
        assertNotNull(
            certFile,
            "Expected $CERT_PKCS7_FILENAME in signed modl"
        )

        // If you want to dump file contents to stdout, uncomment this
        // logZipMapFileText(SIG_PROPERTIES_FILENAME, sigPropsFile)
        // logZipMapFileText(CERT_PKCS7_FILENAME, certFile)
    }

    // This is a test with an actual PKCS#11-compliant YubiKey 5, on Windows.
    // As such it is typically set to @Ignore.
    @Test
    @Ignore
    // @Tag("integration") // break out into a test suite at some point
    // @Tag("IGN-7871")
    fun `integration - module signed with physical pkcs11 HSM on cmdline`() {
        val dirName = currentMethodName()
        val workingDir: File = tempFolder.newFolder(dirName)

        val projectDir = generateModule(workingDir)

        // Write PKCS#11 config file and and cert file, and specify them in
        // gradle.properties.
        val signingResourcesDestination =
            workingDir.toPath().resolve("i-was-signed")
        val (certPath, _) = writeResourceFiles(
            signingResourcesDestination,
            listOf("pkcs11-yk5-win.crt", "pkcs11-yk5-win.cfg")
        )

        val taskArgs = listOf(
            ":signModule",
            // hack around YK5 (signing) slot 9c's second PIN challenge on top
            // of initial keystore PIN challenge by using the cert in (auth)
            // slot 9a
            // "--certAlias=X.509 Certificate for Digital Signature",
            "--certAlias=X.509 Certificate for PIV Authentication",
            "--keystorePassword=123456",
            "--certFile=$certPath",
            // ignored for now, but may be used in future for slot 9c
            "--certPassword=password",
            "--pkcs11CfgFile=./pkcs11-yk5-win.cfg",
            "--stacktrace",
        )

        val result: BuildResult = runTask(
            projectDir.toFile(),
            taskArgs
        )

        val task = result.task(":signModule")
        assertEquals(task?.outcome, TaskOutcome.SUCCESS)

        val buildDir = projectDir.resolve("build")
        val signedFileName = signedModuleName(MODULE_NAME)

        val signed = File("${buildDir.toAbsolutePath()}/$signedFileName")

        // unzip and look for signatures properties file
        val zm = ZipMap(signed)
        val sigPropsFile = zm[SIG_PROPERTIES_FILENAME]
        assertTrue(signed.exists(), "Expected $signed to exist")
        assertNotNull(
            sigPropsFile,
            "Expected $SIG_PROPERTIES_FILENAME in signed modl"
        )

        // and the cert file
        val certFile = zm[CERT_PKCS7_FILENAME]
        assertNotNull(
            certFile,
            "Expected $CERT_PKCS7_FILENAME in signed modl"
        )

        // If you want to dump file contents to stdout, uncomment this
        // logZipMapFileText(SIG_PROPERTIES_FILENAME, sigPropsFile)
        // logZipMapFileText(CERT_PKCS7_FILENAME, certFile)
    }

    private fun generateModule(
        projDir: File,
        skipSigning: Boolean = false,
    ): Path {
        val config = GeneratorConfigBuilder()
            .moduleName(MODULE_NAME)
            .scopes("GCD")
            .packageName("check.my.signage")
            .parentDir(projDir.toPath())
            .debugPluginConfig(true)
            .allowUnsignedModules(skipSigning)
            .settingsDsl(GradleDsl.GROOVY)
            .rootPluginConfig(
                """
                    id("io.ia.sdk.modl")
                """.trimIndent()
            )
            .build()

        return ModuleGenerator.generate(config)
    }

    private fun logZipMapFileText(key: String, file: ZipMapFile) {
        println("$key text:")
        println(file.bytes.toString(Charsets.UTF_8))
    }
}
