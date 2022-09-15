package io.ia.ignition.module.generator.data

import java.nio.file.Files
import java.nio.file.Path
import javax.lang.model.SourceVersion

/**
 * Validates the name intended to be used as the human readable module name.
 *
 * This name should begin with a letter, and include nothing but spaces, letters and numbers.  It should _not_ include
 * a "Module" suffix, and ideally is not excessively long (32 characters  This name will undergo processing to initialize the values and names used in the generated stub
 * files in the new project.  The default generated file/package names may always be modified after generation.
 *
 * Valid Names:
 *     Web Connector
 *     Bridge Perspective Component
 *     SpecialPump Device Driver
 *     Faster Playback
 *     Smartchart10x
 *
 * Invalid Names:
 *     1st Character Is A Number
 *     _Crafty Module_
 *      Started With A Space
 *     Contains F&nky Illegal Characters
 */
fun validateModuleName(name: String): ValidationResult {
    return when {
        name.endsWith("Module") ->
            ValidationResult(false, "The module name $name ends with the suffix \"Module\".")
        !name.matches(Regex("^[a-zA-Z][a-zA-Z0-9 ]*")) ->
            ValidationResult(false, "The module name $name contains illegal characters or does not start with a letter.")
        else -> {
            var message = "The module name $name is valid."
            if (name.length > 32) {
                message += " The module name is excessively long, consider renaming."
            }
            ValidationResult(true, message)
        }
    }
}

/**
 * Validates the package path. Checks if the path is a valid qualified path
 */
fun validatePackagePath(packagePath: String): ValidationResult {
    return if (!SourceVersion.isName(packagePath)) {
        ValidationResult(false, "The package path $packagePath is not a valid path.")
    } else {
        ValidationResult(true, "The package path $packagePath is valid.")
    }
}

/**
 * Validates the parent directory path.
 * Checks whether the path exists, is a directory, and if the directory is writable.
 */
fun validateParentDirPath(path: Path): ValidationResult {
    return when {
        !Files.exists(path) -> ValidationResult(false, "The parent path $path does not exist.")
        !Files.isDirectory(path) -> ValidationResult(false, "The parent path $path is not a directory.")
        !Files.isWritable(path) -> ValidationResult(false, "The parent path $path is not writeable.")
        else -> ValidationResult(true, "The parent path $path is validated.")
    }
}

/**
 * Validates the provided scope string. Will return valid if the scope string contains
 * at least one valid scope or if the scope string contains valid duplicates
 *
 * Valid Scopes:
 *      G
 *      GC
 *      GDC
 *      GDDCC
 *      ABC
 *
 * Invalid Scopes:
 *      123
 *      XYZ
 *      jkl
 * @param scope scope string: G - Gateway, D - Designer, C - Client
 * @return
 */
fun validateScope(scope: String): ValidationResult {
    val (valid, invalid) = scope.partition { it == 'G' || it == 'D' || it == 'C' }
    return if (valid.isNotEmpty()) {
        val message: String = StringBuilder().apply {
            append("We found ${ valid.toSet().joinToString() }")

            if (invalid.isNotEmpty()) {
                append(", but additional unrecognized characters were found and ignored in the scope parameter(s) ${ invalid.toSet().joinToString() }.")
            }
        }.toString()
        ValidationResult(true, message)
    } else {
        ValidationResult(false, "No valid scopes were found in $scope.")
    }
}
