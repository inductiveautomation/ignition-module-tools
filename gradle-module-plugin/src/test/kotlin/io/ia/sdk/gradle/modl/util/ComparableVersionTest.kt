package io.ia.sdk.gradle.modl.util

import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * Test ComparableVersion.
 *
 * @author [Hervé Boutemy](mailto:hboutemy@apache.org)
 */
class ComparableVersionTest {
    private fun newComparable(version: String): ComparableVersion {
        val ver = ComparableVersion(version)
        val canonical = ver.getCanonical()
        val parsedCanonical = ComparableVersion(canonical).getCanonical()
        println("$version.canonical() = $canonical, -> parsedCanonical =  $parsedCanonical")
        assertEquals(canonical, parsedCanonical, "canonical and parsedCanonical should be equal.")
        return ver
    }

    private fun checkVersionsOrder(versions: Array<String>) {
        val c: List<ComparableVersion> = versions.indices.mapIndexed { i, _ -> newComparable(versions[i])}

        for (i in 1 until versions.size) {
            val low = c[i - 1]
            for (j in i until versions.size) {
                val high = c[j]
                assertTrue(low.compareTo(high) < 0, "expected $low < $high")
                assertTrue(high.compareTo(low) > 0, "expected $high > $low")
            }
        }
    }

    private fun checkVersionsEqual(v1: String, v2: String) {
        val c1 = newComparable(v1)
        val c2 = newComparable(v2)
        assertEquals(0, c1.compareTo(c2),  "expected $v1 == $v2")
        assertEquals(0, c2.compareTo(c1), "expected $v2 == $v1")
        assertEquals(c1.hashCode(), c2.hashCode(), "expected same hashcode for $v1 and $v2")
        assertEquals(c1, c2, "expected $v1.equals( $v2 )")
        assertEquals(c2, c1, "expected $v2.equals( $v1 )")
    }

    private fun checkVersionsArrayEqual(array: Array<String>) {
        // compare against each other (including itself)
        for (i in array.indices) for (j in i until array.size) checkVersionsEqual(array[i], array[j])
    }

    private fun checkVersionsOrder(v1: String, v2: String) {
        val c1 = newComparable(v1)
        val c2 = newComparable(v2)
        assertTrue(c1.compareTo(c2) < 0, "expected $v1 < $v2")
        assertTrue(c2.compareTo(c1) > 0, "expected $v2 > $v1")
    }

    @Test
    fun testVersionsQualifier() {
        checkVersionsOrder(VERSIONS_QUALIFIER)
    }

    @Test
    fun testVersionsNumber() {
        checkVersionsOrder(VERSIONS_NUMBER)
    }

    @Test
    fun testVersionsEqual() {
        newComparable("1.0-alpha")
        checkVersionsEqual("1", "1")
        checkVersionsEqual("1", "1.0")
        checkVersionsEqual("1", "1.0.0")
        checkVersionsEqual("1.0", "1.0.0")
        checkVersionsEqual("1", "1-0")
        checkVersionsEqual("1", "1.0-0")
        checkVersionsEqual("1.0", "1.0-0")
        // no separator between number and character
        checkVersionsEqual("1a", "1-a")
        checkVersionsEqual("1a", "1.0-a")
        checkVersionsEqual("1a", "1.0.0-a")
        checkVersionsEqual("1.0a", "1-a")
        checkVersionsEqual("1.0.0a", "1-a")
        checkVersionsEqual("1x", "1-x")
        checkVersionsEqual("1x", "1.0-x")
        checkVersionsEqual("1x", "1.0.0-x")
        checkVersionsEqual("1.0x", "1-x")
        checkVersionsEqual("1.0.0x", "1-x")

        // aliases
        checkVersionsEqual("1ga", "1")
        checkVersionsEqual("1release", "1")
        checkVersionsEqual("1final", "1")
        checkVersionsEqual("1cr", "1rc")

        // special "aliases" a, b and m for alpha, beta and milestone
        checkVersionsEqual("1a1", "1-alpha-1")
        checkVersionsEqual("1b2", "1-beta-2")
        checkVersionsEqual("1m3", "1-milestone-3")

        // case insensitive
        checkVersionsEqual("1X", "1x")
        checkVersionsEqual("1A", "1a")
        checkVersionsEqual("1B", "1b")
        checkVersionsEqual("1M", "1m")
        checkVersionsEqual("1Ga", "1")
        checkVersionsEqual("1GA", "1")
        checkVersionsEqual("1RELEASE", "1")
        checkVersionsEqual("1release", "1")
        checkVersionsEqual("1RELeaSE", "1")
        checkVersionsEqual("1Final", "1")
        checkVersionsEqual("1FinaL", "1")
        checkVersionsEqual("1FINAL", "1")
        checkVersionsEqual("1Cr", "1Rc")
        checkVersionsEqual("1cR", "1rC")
        checkVersionsEqual("1m3", "1Milestone3")
        checkVersionsEqual("1m3", "1MileStone3")
        checkVersionsEqual("1m3", "1MILESTONE3")
    }

    @Test
    fun testVersionComparing() {
        checkVersionsOrder("1", "2")
        checkVersionsOrder("1.5", "2")
        checkVersionsOrder("1", "2.5")
        checkVersionsOrder("1.0", "1.1")
        checkVersionsOrder("1.1", "1.2")
        checkVersionsOrder("1.0.0", "1.1")
        checkVersionsOrder("1.0.1", "1.1")
        checkVersionsOrder("1.1", "1.2.0")
        checkVersionsOrder("1.0-alpha-1", "1.0")
        checkVersionsOrder("1.0-alpha-1", "1.0-alpha-2")
        checkVersionsOrder("1.0-alpha-1", "1.0-beta-1")
        checkVersionsOrder("1.0-beta-1", "1.0-SNAPSHOT")
        checkVersionsOrder("1.0-SNAPSHOT", "1.0")
        checkVersionsOrder("1.0-alpha-1-SNAPSHOT", "1.0-alpha-1")
        checkVersionsOrder("1.0", "1.0-1")
        checkVersionsOrder("1.0-1", "1.0-2")
        checkVersionsOrder("1.0.0", "1.0-1")
        checkVersionsOrder("2.0-1", "2.0.1")
        checkVersionsOrder("2.0.1-klm", "2.0.1-lmn")
        checkVersionsOrder("2.0.1", "2.0.1-xyz")
        checkVersionsOrder("2.0.1", "2.0.1-123")
        checkVersionsOrder("2.0.1-xyz", "2.0.1-123")
    }

    /**
     * Test [MNG-5568](https://issues.apache.org/jira/browse/MNG-5568) edge case
     * which was showing transitive inconsistency: since A &gt; B and B &gt; C then we should have A &gt; C
     * otherwise sorting a list of ComparableVersions() will in some cases throw runtime exception;
     * see Netbeans issues [240845](https://netbeans.org/bugzilla/show_bug.cgi?id=240845) and
     * [226100](https://netbeans.org/bugzilla/show_bug.cgi?id=226100)
     */
    @Test
    fun testMng5568() {
        val a = "6.1.0"
        val b = "6.1.0rc3"
        val c = "6.1H.5-beta" // this is the unusual version string, with 'H' in the middle
        checkVersionsOrder(b, a) // classical
        checkVersionsOrder(b, c) // now b < c, but before MNG-5568, we had b > c
        checkVersionsOrder(a, c)
    }

    /**
     * Test [MNG-6572](https://jira.apache.org/jira/browse/MNG-6572) optimization.
     */
    @Test
    fun testMng6572() {
        val a = "20190126.230843" // resembles a SNAPSHOT
        val b = "1234567890.12345" // 10 digit number
        val c = "123456789012345.1H.5-beta" // 15 digit number
        val d = "12345678901234567890.1H.5-beta" // 20 digit number
        checkVersionsOrder(a, b)
        checkVersionsOrder(b, c)
        checkVersionsOrder(a, c)
        checkVersionsOrder(c, d)
        checkVersionsOrder(b, d)
        checkVersionsOrder(a, d)
    }

    /**
     * Test all versions are equal when starting with many leading zeroes regardless of string length
     * (related to MNG-6572 optimization)
     */
    @Test
    fun testVersionEqualWithLeadingZeroes() {
        // versions with string lengths from 1 to 19
        val arr = arrayOf(
            "0000000000000000001",
            "000000000000000001",
            "00000000000000001",
            "0000000000000001",
            "000000000000001",
            "00000000000001",
            "0000000000001",
            "000000000001",
            "00000000001",
            "0000000001",
            "000000001",
            "00000001",
            "0000001",
            "000001",
            "00001",
            "0001",
            "001",
            "01",
            "1"
        )
        checkVersionsArrayEqual(arr)
    }

    /**
     * Test all "0" versions are equal when starting with many leading zeroes regardless of string length
     * (related to MNG-6572 optimization)
     */
    @Test
    fun testVersionZeroEqualWithLeadingZeroes() {
        // versions with string lengths from 1 to 19
        val arr = arrayOf(
            "0000000000000000000",
            "000000000000000000",
            "00000000000000000",
            "0000000000000000",
            "000000000000000",
            "00000000000000",
            "0000000000000",
            "000000000000",
            "00000000000",
            "0000000000",
            "000000000",
            "00000000",
            "0000000",
            "000000",
            "00000",
            "0000",
            "000",
            "00",
            "0"
        )
        checkVersionsArrayEqual(arr)
    }

    /**
     * Test [MNG-6964](https://issues.apache.org/jira/browse/MNG-6964) edge cases
     * for qualifiers that start with "-0.", which was showing A == C and B == C but A &lt; B.
     */
    @Test
    fun testMng6964() {
        val a = "1-0.alpha"
        val b = "1-0.beta"
        val c = "1"
        checkVersionsOrder(a, c) // Now a < c, but before MNG-6964 they were equal
        checkVersionsOrder(b, c) // Now b < c, but before MNG-6964 they were equal
        checkVersionsOrder(a, b) // Should still be true
    }

    @Test
    fun testLocaleIndependent() {
        val orig = Locale.getDefault()
        val locales = arrayOf(Locale.ENGLISH, Locale("tr"), Locale.getDefault())
        try {
            for (locale in locales) {
                Locale.setDefault(locale)
                checkVersionsEqual("1-abcdefghijklmnopqrstuvwxyz", "1-ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            }
        } finally {
            Locale.setDefault(orig)
        }
    }

    @Test
    fun testReuse() {
        val c1 = ComparableVersion("1")
        c1.parseVersion("2")
        val c2 = newComparable("2")
        assertEquals(c1, c2, "reused instance should be equivalent to new instance")
    }

    companion object {
        private val VERSIONS_QUALIFIER = arrayOf(
            "1-alpha2snapshot", "1-alpha2", "1-alpha-123", "1-beta-2", "1-beta123", "1-m2", "1-m11", "1-rc", "1-cr2",
            "1-rc123", "1-SNAPSHOT", "1", "1-sp", "1-sp2", "1-sp123", "1-abc", "1-def", "1-pom-1", "1-1-snapshot",
            "1-1", "1-2", "1-123"
        )
        private val VERSIONS_NUMBER = arrayOf(
            "2.0", "2-1", "2.0.a", "2.0.0.a", "2.0.2", "2.0.123", "2.1.0", "2.1-a", "2.1b", "2.1-c", "2.1-1", "2.1.0.1",
            "2.2", "2.123", "11.a2", "11.a11", "11.b2", "11.b11", "11.m2", "11.m11", "11", "11.a", "11b", "11c", "11m"
        )
    }
}
