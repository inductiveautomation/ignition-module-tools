package io.ia.ignition.module.generator.util

import io.ia.ignition.module.generator.ModuleGenerator
import io.ia.ignition.module.generator.api.ProjectScope
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

/* This file contains extension functions associated with JVM/Std Lib classes */

/**
 * Assuming the String being acted on is a validated module name provided by the lib, creates the 'class friendly'
 * name that can be used in source code.  This primarily means: Camel Cased, no spaces
 */
fun String.toClassFriendlyName(): String {
    val split = this.split(" ")

    return split.joinToString("") { it.capitalize() }
}

/**
 * Convenience for creating a package folder path from a package name.
 *
 * @param maybeScope optional parameter to get the package path including an appended scope name
 */
fun String.toPackagePath(maybeScope: ProjectScope? = null): String {
    val path = this.replace(".", "/")

    return if (maybeScope == null) {
        path
    } else {
        "$path/${maybeScope.name.toLowerCase()}"
    }
}

fun Path.replacePlaceholders(replacements: Map<String, String> = emptyMap()): Path {
    this.toFile().replacePlaceholders(replacements)
    return this
}

fun File.replacePlaceholders(replacements: Map<String, String> = emptyMap()): String {
    return this.readText().let { ctnt ->
        ctnt.lines().map { line ->
            var updatedLine = line

            replacements.forEach { (key, value) ->
                if (line.contains(key)) {
                    updatedLine = line.replace(key, value)
                }
            }
            updatedLine
        }.joinToString("\n")
    }.also {
        this@replacePlaceholders.writeText(it)
    }
}

fun InputStream.writeToFileWithReplacements(file: File, replacements: Map<String, String> = emptyMap()) {
    BufferedReader(InputStreamReader(this)).use { reader ->
        file.outputStream().use { outStream ->
            while (reader.ready()) {
                var line = reader.readLine()

                if (line != null) {
                    replacements.forEach {
                        if (line.contains(it.key)) {
                            line = line.replace(it.key, it.value)
                        }
                    }
                    outStream.write("$line\n".toByteArray(Charsets.UTF_8))
                }
            }
        }
    }
}

/**
 * Extension function which fills the File with text originating from the given resource
 *
 * It's expected that the given resource is accessible from the current classpath, following the behavior of
 * java.util.ClassLoader.getSystemResourceAsStream()
 *
 * @param resourcePath the path to the resource who's content is intended to fill the 'this' File
 * @see java.lang.ClassLoader.getSystemResourceAsStream
 */
fun File.createAndFillFromResource(resourcePath: String, replacements: Map<String, String> = emptyMap()): File {
    if (!this.exists()) {

        if (!this.parentFile.exists()) {
            this.parentFile.mkdirs()
        }

        this.createNewFile()

        ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath).use { inputStream ->
            inputStream?.writeToFileWithReplacements(this, replacements)
        }
    } else {
        throw IOException("Could not create '${this.absolutePath}', already exists. Skipping content write!")
    }

    return this
}

/**
 * Convenience function which fills the Path with text originating from the given resource by calling
 * toFile().fillFromResource(STRING).  Assumes the Path in question is resolvable as a File.
 *
 * It's expected that the given resource is accessible from the current classpath, following the behavior of
 * java.util.ClassLoader.getSystemResourceAsStream()
 *
 * @param resourcePath the path to the resource who's content is intended to fill the 'this' Path
 * @see File.createAndFillFromResource
 * @return the Path object that was filled
 */
fun Path.createAndFillFromResource(resourcePath: String, replacements: Map<String, String> = emptyMap()): Path {
    return this.toFile().createAndFillFromResource(resourcePath, replacements).toPath()
}

fun Path.copyFromResource(resourcePath: String): Path {
    if (!this.toFile().parentFile.exists()) {
        this.toFile().parentFile.mkdirs()
    }

    println("Loading resource $resourcePath")
    val resPath = ClassLoader.getSystemClassLoader().getResource(resourcePath).file.toString()

    ModuleGenerator::class.java.classLoader.getResourceAsStream(resourcePath).use { inputStream ->
        Files.copy(inputStream, this@copyFromResource)
    }
    return this
}

/**
 * Attempts to read the resource bath and appends its contents to the file located at the Path.
 *  * @param resourcePath the path to the resource who's content is intended to fill the 'this' Path
 * @return the Path object that was appended
 */
fun Path.appendFromResource(resourcePath: String, replacements: Map<String, String> = emptyMap()): Path {
    if (!this.toFile().exists()) {
        throw FileNotFoundException("Could not append to ${this.absolutePathString()}, file did not exist.")
    }

    ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath).use { inputStream ->
        if (inputStream == null) {
            throw FileNotFoundException("Could not read item at Path $resourcePath")
        }
        val contentToAppend = String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
        this@appendFromResource.toFile().appendText("\n$contentToAppend")
    }

    if (replacements.isNotEmpty()) this.replacePlaceholders(replacements)

    return this
}
