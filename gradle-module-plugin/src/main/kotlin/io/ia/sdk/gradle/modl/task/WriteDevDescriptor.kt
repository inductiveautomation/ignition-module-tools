package io.ia.sdk.gradle.modl.task

import io.ia.sdk.gradle.modl.extension.ModuleSettings
import io.ia.sdk.gradle.modl.model.DevModuleDependency
import io.ia.sdk.gradle.modl.model.DevModuleDescriptor
import io.ia.sdk.gradle.modl.model.DevScopeEntry
import io.ia.sdk.gradle.modl.model.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Generates a JSON dev module descriptor for IDE classloader isolation.
 *
 * The descriptor contains module metadata (id, name, version, hooks, dependencies) along with
 * per-scope class directories and dependency JARs collected from each subproject's build output.
 *
 * Class output directories are selected based on the IDE build delegation setting:
 * - If IDEA manages the build (`delegatedBuild=false` in `.idea/gradle.xml`), the descriptor
 *   uses `out/production/classes` paths.
 * - If Gradle manages the build (delegated, or detection fails), the descriptor uses
 *   `build/classes/{java,kotlin}/main` paths.
 * - Fallback: if the detected mode's dirs don't exist but the other mode's do, the existing
 *   dirs are used.
 *
 * All scope data is collected at execution time to avoid configuration-time ordering issues.
 */
open class WriteDevDescriptor @Inject constructor(objects: ObjectFactory) : DefaultTask() {

    companion object {
        const val ID = "writeDevModuleDescriptor"
    }

    init {
        group = "ignition development"
        description = "Generates a dev module descriptor for IDE classloader isolation"
    }

    @get:Input
    val moduleId: Property<String> = objects.property(String::class.java)

    @get:Input
    val moduleName: Property<String> = objects.property(String::class.java)

    @get:Input
    val moduleVersion: Property<String> = objects.property(String::class.java)

    @get:Input
    val freeModule: Property<Boolean> = objects.property(Boolean::class.javaObjectType)

    /** Hook classes as configured in the extension: className -> scopeLetter */
    @get:Input
    val hookClasses: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java)

    /** Project path -> scope letter(s) mapping from the extension */
    @get:Input
    val projectScopes: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java)

    @OutputFile
    fun getOutputFile(): File {
        return project.layout.buildDirectory.file("dev/${moduleId.get()}.json").get().asFile
    }

    @TaskAction
    fun execute() {
        val settings = project.extensions.findByType(ModuleSettings::class.java)
        val ideaBuilds = detectIdeaBuildMode()

        if (ideaBuilds) {
            logger.info("Detected IDEA-managed builds (delegatedBuild=false). Using out/production/ class dirs.")
        } else {
            logger.info("Using Gradle build output class dirs.")
        }

        val descriptor = DevModuleDescriptor(
            id = moduleId.get(),
            name = moduleName.get(),
            version = moduleVersion.get(),
            freeModule = freeModule.get(),
            hooks = hookClasses.get().entries.associate { (className, scope) -> scope to className },
            moduleDependencies = collectDependencySpecs(settings),
            scopes = collectScopeData(ideaBuilds),
            exports = emptyMap()
        )

        val outFile = getOutputFile()
        outFile.parentFile.mkdirs()
        outFile.writeText(descriptor.toJson())
        logger.lifecycle("Wrote dev module descriptor: ${outFile.absolutePath}")
    }

    /**
     * Detects whether IDEA is managing the build by reading `.idea/gradle.xml`.
     * Returns `true` if `delegatedBuild` is explicitly set to `false`.
     */
    private fun detectIdeaBuildMode(): Boolean {
        val gradleXml = project.rootProject.file(".idea/gradle.xml")
        if (!gradleXml.exists()) return false
        return try {
            val content = gradleXml.readText()
            content.contains("""delegatedBuild" value="false""")
        } catch (e: Exception) {
            logger.debug("Could not read .idea/gradle.xml: ${e.message}")
            false
        }
    }

    private fun collectDependencySpecs(settings: ModuleSettings?): List<DevModuleDependency> {
        return settings?.moduleDependencySpecs?.map { spec ->
            DevModuleDependency(
                id = spec.name,
                scope = spec.scope,
                required = spec.required
            )
        } ?: emptyList()
    }

    /**
     * Collects class directories and dependency JARs for each scope.
     *
     * @param ideaBuilds true if IDEA manages the build (use out/production/), false for Gradle (use build/classes/)
     */
    private fun collectScopeData(ideaBuilds: Boolean): Map<String, DevScopeEntry> {
        val scopeMap = mutableMapOf<String, Pair<MutableSet<String>, MutableSet<String>>>()

        projectScopes.get().forEach { (projectPath, scope) ->
            val subproj = project.rootProject.findProject(projectPath) ?: return@forEach
            val (classDirs, jars) = scopeMap.getOrPut(scope) {
                mutableSetOf<String>() to mutableSetOf()
            }

            if (ideaBuilds) {
                // IDEA-managed build: use out/production/classes
                val ideaOutDir = subproj.file("out/production/classes")
                classDirs.add(ideaOutDir.absolutePath)

                // Fallback: if out/ doesn't exist but build/classes does, include build/ too
                if (!ideaOutDir.isDirectory) {
                    addGradleClassDirs(subproj, classDirs)
                }
            } else {
                // Gradle-delegated build: use build/classes/*/main
                addGradleClassDirs(subproj, classDirs)

                // Fallback: if build/classes doesn't exist but out/ does, include out/
                val buildClassesDir = subproj.file("build/classes")
                if (!buildClassesDir.isDirectory) {
                    val ideaOutDir = subproj.file("out/production/classes")
                    if (ideaOutDir.isDirectory) {
                        classDirs.add(ideaOutDir.absolutePath)
                    }
                }
            }

            // Resources — always from Gradle's build dir (IDEA uses the same path)
            val resourcesDir = subproj.file("build/resources/main")
            classDirs.add(resourcesDir.absolutePath)

            // Resolved dependency JARs from collectModlDependencies output
            val artifactsDir = subproj.file("build/artifacts")
            val subprojName = subproj.name
            if (artifactsDir.isDirectory) {
                artifactsDir.listFiles()
                    ?.filter { it.name.endsWith(".jar") && !it.name.startsWith(subprojName) }
                    ?.forEach { jar -> jars.add(jar.absolutePath) }
            }
        }

        return scopeMap.mapValues { (_, pair) ->
            DevScopeEntry(classDirs = pair.first, jars = pair.second)
        }
    }

    /** Adds Gradle class output directories (build/classes/java/main, build/classes/kotlin/main, etc.) */
    private fun addGradleClassDirs(subproj: org.gradle.api.Project, classDirs: MutableSet<String>) {
        val javaExt = subproj.extensions.findByType(org.gradle.api.plugins.JavaPluginExtension::class.java)
        javaExt?.sourceSets?.findByName("main")?.let { mainSourceSet ->
            mainSourceSet.output.classesDirs.files.forEach { dir ->
                classDirs.add(dir.absolutePath)
            }
        }
    }
}
