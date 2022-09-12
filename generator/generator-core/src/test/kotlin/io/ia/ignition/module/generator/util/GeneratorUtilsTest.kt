package io.ia.ignition.module.generator.util

import io.ia.ignition.module.generator.api.ProjectScope
import io.ia.ignition.module.generator.api.SourceFileType.JAVA
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GeneratorUtilsTest {
    private val logger: Logger = LoggerFactory.getLogger(GeneratorUtilsTest::class.java)

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `creates valid subproject dirs`() {
        val rootTestDir = tempFolder.newFolder().absoluteFile.toPath()
        val testPackagePath = "com/inductiveautomation/example/client"

        // check tests for valid structure and content
        ProjectScope.values().forEach {

            val subProjectRootDir = rootTestDir.resolve(it.folderName)
            val created = createSourceDirs(subProjectRootDir, testPackagePath, JAVA)

            // what we want to have created
            val expectedPackagePath = "src/main/${JAVA.commonName()}/$testPackagePath/"
            val testFolder = File(subProjectRootDir.toFile(), expectedPackagePath)

            assertTrue(created.toFile().exists())
            assertEquals(testFolder.absolutePath, created.toString())
        }
    }

    @Test
    fun `class name is correctly built from valid module name`() {
        val validModuleNamesToExpectedClassFormat = mapOf(
            "Some Functionality" to "SomeFunctionality",
            "some functionality" to "SomeFunctionality",
            "Do The 123 Things" to "DoThe123Things",
            "I do cool stuff" to "IDoCoolStuff",
            "More Test Strings" to "MoreTestStrings"
        )

        logger.debug("Executing ")
        validModuleNamesToExpectedClassFormat.forEach {

            val generatedClassName = it.key.toClassFriendlyName()
            logger.debug("Generated classname for ${it.key} was $generatedClassName")

            assertEquals(
                generatedClassName,
                it.value,
                "Generated classname from module name matched expected value"
            )
        }
    }
}
