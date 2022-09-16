package io.ia.ignition.module.generator.data

import org.junit.Test
import kotlin.test.assertEquals

class ValidationTest {

    companion object {
        val validNames = listOf(
            "Some Name",
            "Great Service Provider",
            "OMRON",
            "Great SCOTT",
            "awesome functionality"
        )
    }

    @Test
    fun `valid module names pass validation`() {

        validNames.forEach {
            assertEquals(
                ValidationResult(true, "The module name $it is valid."),
                validateModuleName(it)
            )
        }
    }

    @Test
    fun `valid module name long`() {
        val moduleName = "Some REEEEEAAALLLLLLLLLLLY LOOOOOOOOOOOOOOOONG MODULE Name"
        assertEquals(
            ValidationResult(
                true,
                "The module name $moduleName is valid. The module name is excessively long," +
                    " consider renaming."
            ),
            validateModuleName(moduleName)
        )
    }

    @Test
    fun `invalid module name suffix`() {
        val moduleName = "Bad Module"
        assertEquals(
            ValidationResult(false, "The module name Bad Module ends with the suffix \"Module\"."),
            validateModuleName(moduleName)
        )
    }

    @Test
    fun `invalid module name invalid characters`() {
        val moduleName = "_Crafty Module_"
        assertEquals(
            ValidationResult(
                false,
                "The module name $moduleName contains illegal characters or does not start with a letter."
            ),
            validateModuleName(moduleName)
        )
    }

    @Test
    fun `invalid module name not starting with letter`() {
        val moduleName = "1st Component"
        assertEquals(
            ValidationResult(
                false,
                "The module name $moduleName contains illegal characters or does not start with a letter."
            ),
            validateModuleName(moduleName)
        )
    }

    @Test
    fun `valid package path`() {
        val packagePath = "my.awesome.module"
        assertEquals(
            ValidationResult(true, "The package path $packagePath is valid."),
            validatePackagePath(packagePath)
        )
    }

    @Test
    fun `invalid package path`() {
        listOf(
            "contains.keyword.int.true.path",
            "contains@bad-separators",
            "too...many..separators"
        ).forEach {
            assertEquals(
                ValidationResult(false, "The package path $it is not a valid path."),
                validatePackagePath(it)
            )
        }
    }

    @Test
    fun `valid scope`() {
        val scope = "GCD"

        assertEquals(
            ValidationResult(true, "We found G, C, D"),
            validateScope(scope)
        )
    }

    @Test
    fun `valid scope with additional invalid scopes`() {
        val scope = "GCDAB"

        assertEquals(
            ValidationResult(true, "We found G, C, D, but additional unrecognized characters were found and ignored in the scope parameter(s) A, B."),
            validateScope(scope)
        )
    }

    @Test
    fun `invalid scope values`() {

        listOf("_XYZ", "", "123", "#", "{}").forEach {
            assertEquals(
                ValidationResult(false, "No valid scopes were found in $it."),
                validateScope(it)
            )
        }
    }
}
