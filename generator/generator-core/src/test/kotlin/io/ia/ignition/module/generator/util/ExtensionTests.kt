package io.ia.ignition.module.generator.util

import io.ia.ignition.module.generator.api.ProjectScope.CLIENT
import io.ia.ignition.module.generator.api.ProjectScope.GATEWAY
import java.io.File
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Unit tests supporting extension functions
 */
class ExtensionTests {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `valid package roots correctly convert to paths`() {
        // example package names to expected folder path strings
        val packageRootsToPaths = mapOf(
            "le.examp.test" to "le/examp/test",
            "com.test.path" to "com/test/path",
            "io.ia.great" to "io/ia/great",
            "com" to "com",
            "net.explor.in.long.package.name" to "net/explor/in/long/package/name"
        )

        packageRootsToPaths.keys.forEach {
            val path = it.toPackagePath()

            assertEquals(path, packageRootsToPaths[it])
        }
    }

    @Test
    fun `File fill from resource writes multiline content`() {
        val checkPhrase = "I'm sittin' on the dock of the bay\nWastin' time"

        val testFolder = tempFolder.newFolder()
        val testFile = File(testFolder, "destination.txt")

        testFile.createAndFillFromResource("fillFromResourceTest.txt")

        val readBack = testFile.readText()

        assertTrue(readBack.contains(checkPhrase))
    }

    @Test
    fun `File fill from resource swaps provided terms`() {
        val checkPhrase1 = "Nothing still remains the same"
        val checkPhrase2 = "Watching the covid roll in"

        val swaps = mapOf("Everything" to "Nothing", "ships" to "covid")

        val testFolder = tempFolder.newFolder()
        val testFile = File(testFolder, "destination.txt")

        testFile.createAndFillFromResource("fillFromResourceTest.txt", swaps)

        val readBack = testFile.readText()

        assertTrue(readBack.contains(checkPhrase1))
        assertTrue(readBack.contains(checkPhrase2))
    }

    @Test
    fun `Path fill from resource writes multiline content`() {
        val checkPhrase = "I'm sittin' on the dock of the bay\nWastin' time"

        val testFolder = tempFolder.newFolder()
        val testFile = File(testFolder, "destination.txt")

        testFile.createAndFillFromResource("fillFromResourceTest.txt")

        val readBack = testFile.readText()

        assertTrue(readBack.contains(checkPhrase))
    }

    @Test
    fun `Path fill from resource swaps provided terms`() {
        val checkPhrase1 = "Nothing still remains the same"
        val checkPhrase2 = "Panic as the covid rolls in"
        val swaps = mapOf("Everything" to "Nothing", "Watching the ships roll in" to checkPhrase2)

        val testFolder = tempFolder.newFolder().toPath()
        val testFile = testFolder.resolve("destination.txt")

        testFile.createAndFillFromResource("fillFromResourceTest.txt", swaps)

        val readBack = testFile.toFile().readText(Charsets.UTF_8)

        assertTrue(readBack.contains(checkPhrase1))
        assertTrue(readBack.contains(checkPhrase2))
    }

    @Test
    fun `Path append from resource appends`() {
        val checkPhrase1 = "i am some appended text content"

        val testFolder = tempFolder.newFolder().toPath()
        val testFile = testFolder.resolve("destination.txt")

        testFile.createAndFillFromResource("fillFromResourceTest.txt")
            .appendFromResource("appendedResource.txt")

        val readBack = testFile.toFile().readText(Charsets.UTF_8)

        assertTrue(readBack.contains(checkPhrase1))
    }

    @Test
    fun `Path append from resource appends and applies replacement map`() {
        val checkPhrase1 = "i am working!"
        val checkPhrase2 = "Sleepin' in the morning sun"
        val swaps = mapOf("Sittin" to "Sleepin", "some appended text content" to "working!")

        val testFolder = tempFolder.newFolder().toPath()
        val testFile = testFolder.resolve("destination.txt")

        testFile.createAndFillFromResource("fillFromResourceTest.txt")
            .appendFromResource("appendedResource.txt", swaps)

        val readBack = testFile.toFile().readText(Charsets.UTF_8)

        assertTrue(readBack.contains(checkPhrase1))
        assertTrue(readBack.contains(checkPhrase2))
    }

    @Test
    fun `package roots correctly convert to package paths`() {
        val package1 = "com.inductiveautomation.example.pkg"
        val expected1 = "com/inductiveautomation/example/pkg"
        val expectedScoped1 = "com/inductiveautomation/example/pkg/client"
        val package2 = "le.examp1"
        val expected2 = "le/examp1"
        val expectedScoped2 = "le/examp1/gateway"

        assertEquals(expected1, package1.toPackagePath())
        assertEquals(expectedScoped1, package1.toPackagePath(CLIENT))
        assertEquals(expected2, package2.toPackagePath())
        assertEquals(expectedScoped2, package2.toPackagePath(GATEWAY))
    }

    @Test
    fun `human readable module name converts to valid class name`() {
        val sampleNames = listOf(
            "My Cool Thing",
            "Wicked Fast Network Mover",
            "In Memory Historian",
            "the greatest service",
            "a Not so Good service"
        )

        val expectedResults = listOf(
            "MyCoolThing",
            "WickedFastNetworkMover",
            "InMemoryHistorian",
            "TheGreatestService",
            "ANotSoGoodService"
        )

        sampleNames.forEachIndexed { index, sample ->
            assertEquals(expectedResults[index], sample.toClassFriendlyName())
        }
    }
}
