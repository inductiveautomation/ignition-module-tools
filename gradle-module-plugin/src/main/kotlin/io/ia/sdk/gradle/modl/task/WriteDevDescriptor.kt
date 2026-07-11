package io.ia.sdk.gradle.modl.task

import io.ia.sdk.gradle.modl.extension.ModuleDependencySpec
import io.ia.sdk.gradle.modl.model.DevModuleDependency
import io.ia.sdk.gradle.modl.model.DevModuleDescriptor
import io.ia.sdk.gradle.modl.model.DevScopeEntry
import io.ia.sdk.gradle.modl.model.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Generates a JSON dev module descriptor for IDE classloader isolation.
 *
 * The descriptor contains module metadata (id, name, version, hooks, dependencies) along with
 * per-scope class directories and dependency JARs collected from each subproject's build output.
 *
 * All project-graph data (per-scope class output roots and dependency jars) is captured into task
 * inputs at configuration time via [scopeInputs] and [moduleDependencySpecs], so the task action
 * never touches the `Project` object at execution time. This keeps the task compatible with
 * Gradle's configuration cache.
 *
 * Both the Gradle build output roots (`build/classes/{java,kotlin}/main`, `build/resources/main`)
 * and the IDEA-managed roots (`out/production/{classes,resources}`) are captured up front; at
 * execution time only the directories that actually exist on disk are written into the descriptor,
 * so no `.idea/gradle.xml` IDE-mode detection is needed.
 */
@CacheableTask
open class WriteDevDescriptor @Inject constructor(objects: ObjectFactory) : DefaultTask() {

    /**
     * A single project's contribution to one Ignition scope. Multiple entries may share the same
     * [scope] when more than one subproject targets it; they are merged at execution time.
     */
    data class ScopeInput(
        @get:Input
        val scope: String,

        @get:Input
        val projectName: String,

        // lazy Gradle types are CC-serializable; classes dirs feed both @InputFiles and the descriptor
        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val classesDirs: FileCollection,

        @get:InputFiles
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val artifactJars: FileCollection,
    )

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

    /** Module dependency specs captured from the extension at configuration time. */
    @get:Input
    val moduleDependencySpecs: SetProperty<ModuleDependencySpec> =
        objects.setProperty(ModuleDependencySpec::class.java)

    /** Per-project, per-scope class dirs and dependency jars, captured at configuration time. */
    @get:Nested
    val scopeInputs: ListProperty<ScopeInput> = objects.listProperty(ScopeInput::class.java)

    @get:OutputFile
    val outPutFile: RegularFileProperty = objects.fileProperty().convention(
        project.layout.buildDirectory.file(moduleId.map { "dev/$it.json" }),
    )

    @TaskAction
    fun execute() {
        val descriptor = DevModuleDescriptor(
            id = moduleId.get(),
            name = moduleName.get(),
            version = moduleVersion.get(),
            freeModule = freeModule.get(),
            hooks = hookClasses.get().entries.associate { (className, scope) -> scope to className },
            moduleDependencies = collectDependencySpecs(),
            scopes = collectScopeData(),
            exports = emptyMap(),
        )

        val outFile = outPutFile.get().asFile
        outFile.parentFile.mkdirs()
        outFile.writeText(descriptor.toJson())
        logger.lifecycle("Wrote dev module descriptor: ${outFile.absolutePath}")
    }

    private fun collectDependencySpecs(): List<DevModuleDependency> = moduleDependencySpecs.get().map { spec ->
        DevModuleDependency(
            id = spec.name,
            scope = spec.scope,
            required = spec.required,
        )
    }

    /**
     * Merges the captured [scopeInputs] into a per-scope map of class dirs and dependency jars.
     * Only class directories that exist on disk are included, which transparently covers both
     * IDEA-managed (out/production) and Gradle-managed (build/classes) build outputs.
     */
    private fun collectScopeData(): Map<String, DevScopeEntry> {
        val scopeMap = mutableMapOf<String, Pair<MutableSet<String>, MutableSet<String>>>()

        scopeInputs.get().forEach { input ->
            val (classDirs, jars) = scopeMap.getOrPut(input.scope) {
                mutableSetOf<String>() to mutableSetOf()
            }

            input.classesDirs.files
                .filter { it.exists() }
                .forEach { classDirs.add(it.absolutePath) }

            input.artifactJars.files
                .filter { it.name.endsWith(".jar") && !it.name.startsWith(input.projectName) }
                .forEach { jars.add(it.absolutePath) }
        }

        return scopeMap.mapValues { (_, pair) ->
            DevScopeEntry(classDirs = pair.first, jars = pair.second)
        }
    }
}
