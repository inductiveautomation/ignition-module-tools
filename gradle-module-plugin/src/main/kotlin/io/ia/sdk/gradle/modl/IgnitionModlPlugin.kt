package io.ia.sdk.gradle.modl

import io.ia.sdk.gradle.modl.api.Constants.APPLY_IA_REPOSITORY_FLAG
import io.ia.sdk.gradle.modl.api.Constants.MODULE_API_CONFIGURATION
import io.ia.sdk.gradle.modl.api.Constants.MODULE_IMPLEMENTATION_CONFIGURATION
import io.ia.sdk.gradle.modl.extension.EXTENSION_NAME
import io.ia.sdk.gradle.modl.extension.ModuleSettings
import io.ia.sdk.gradle.modl.task.AssembleModuleAssets
import io.ia.sdk.gradle.modl.task.CollectModlDependencies
import io.ia.sdk.gradle.modl.task.SignModule
import io.ia.sdk.gradle.modl.task.WriteModuleXml
import io.ia.sdk.gradle.modl.task.ZipModule
import io.ia.sdk.gradle.modl.task.ZipModule.Companion.UNSIGNED_EXTENSION
import io.ia.sdk.gradle.modl.util.hasOptedOutOfModule
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskProvider

/**
 * Group used by all tasks so they show up in the appropriate category when 'gradle tasks' is executed
 */
const val PLUGIN_TASK_GROUP: String = "Ignition Module"

/**
 * The primary plugin class, which is the entry point for the plugin.
 */
class IgnitionModlPlugin : Plugin<Project> {

    private lateinit var appliedPluginProject: Project

    companion object {
        const val PLUGIN_ID = "io.ia.sdk.modl"
        const val PROJECT_OPT_OUT = "excludeFromIgnitionModule"
    }

    override fun apply(project: Project) {
        this.appliedPluginProject = project

        // check for and set base plugin if not present, to establish lifecycle tasks/hooks we'll use
        if (!project.plugins.hasPlugin("base")) {
            project.plugins.apply("base")
        }

        addInductiveAutoRepos(project)

        // create the extension object used to configure the plugin in the user's buildscripts
        val settings = project.extensions.create(
            EXTENSION_NAME,
            ModuleSettings::class.java
        )

        setupRootTasks(project, settings)
        setupSubprojectTasks(project, settings)
    }

    /**
     * Sets up the necessary tasks and configurations for subprojects, adding task dependencies to the 'root' project as
     * necessary.
     */
    private fun setupSubprojectTasks(root: Project, settings: ModuleSettings) {
        root.subprojects.forEach { p: Project ->
            // allow projects to declare themselves exempt from the module plugin functionality by setting a property
            // in the build.gradle `ext { disableModulePlugin=true }
            if (p.hasOptedOutOfModule()) {
                return
            }

            // apply the base plugin to establish a baseline lifecycle on all projects
            p.plugins.apply("base")

            // ignition modules are built using configurations from gradle's 'java-library' plugin to allow proper
            // transitive dependency management, so we look for that plugin to be applied
            p.plugins.withType(JavaLibraryPlugin::class.java) {
                createConfigurations(p)
                createJavaTasks(p, this.appliedPluginProject, settings)
            }

            root.tasks.findByName("assemble")?.dependsOn(p.tasks.findByName("assemble"))
        }
    }

    /**
     * Initializes tasks associated with the root module project, and returns a map of all tasks keyed on the task name
     */
    private fun setupRootTasks(root: Project, settings: ModuleSettings) {
        root.logger.info("Initializing tasks on root module project...")

        val rootAssemble = root.tasks.findByName("assemble")

        // task that gathers dependencies and assets into a folder that will ultimately become the .modl contents
        val assembleModuleStructure = root.tasks.register(
            AssembleModuleAssets.ID,
            AssembleModuleAssets::class.java
        ) {
            it.moduleContentDir.set(root.layout.buildDirectory.dir("moduleContent"))
            it.license.set(settings.license)
        }

        val writeModuleXml = root.tasks.register(
            WriteModuleXml.ID,
            WriteModuleXml::class.java
        ) { xmlTask: WriteModuleXml ->

            // bind configuration settings values to the task input properties for incremental build support
            xmlTask.license.set(settings.license)
            xmlTask.hookClasses.set(settings.hooks)
            xmlTask.moduleDescription.set(settings.moduleDescription)
            xmlTask.moduleId.set(settings.id)
            xmlTask.moduleName.set(settings.name)
            xmlTask.moduleVersion.set(settings.moduleVersion)
            xmlTask.moduleDependencies.set(settings.moduleDependencies)
            xmlTask.requiredIgnitionVersion.set(settings.requiredIgnitionVersion)
            xmlTask.requiredFrameworkVersion.set(settings.requiredFrameworkVersion)
            xmlTask.requireFromPlatform.set(settings.requireFromPlatform)
            xmlTask.freeModule.set(settings.freeModule)

            // xml task depends on having module structure
            xmlTask.dependsOn(assembleModuleStructure)
        }

        // task that zips up the folder of module content
        val zip = root.tasks.register(
                ZipModule.ID,
                ZipModule::class.java
        ) { zipTask: ZipModule ->
            zipTask.content.set(assembleModuleStructure.flatMap { it.moduleContentDir })
            zipTask.moduleName.set(settings.name)
            zipTask.unsignedModule.set(settings.fileName.flatMap {
                val fileName = if (it.endsWith(".modl")) {
                    it.replace(".modl", ".$UNSIGNED_EXTENSION")
                } else {
                    "$it.$UNSIGNED_EXTENSION"
                }
                root.layout.buildDirectory.file(fileName) }
            )

            // need xml file written before we zip anything
            zipTask.dependsOn(writeModuleXml)
        }

        // task that signs the module, using [http://github.com/inductiveautomation/module-signer] to do so
        val sign = root.tasks.register(SignModule.ID, SignModule::class.java) { signTask ->
            signTask.unsigned.set(zip.flatMap { it.unsignedModule })
            signTask.propertyFilePath.set(settings.propertyFile)
        }

        root.allprojects.forEach { p ->
            // when a project has a task of the given name, apply appropriate task dependencies
            p.tasks.whenTaskAdded { t ->
                when (t.name) {
                    CollectModlDependencies.ID -> {
                        val gatherArtifacts: CollectModlDependencies = t as CollectModlDependencies

                        // bind artifact collection task output to xml writing task input
                        writeModuleXml.configure {
                            it.artifactManifests.add(gatherArtifacts.manifestFile)
                        }

                        // bind artifact collection dir to the module content collection task input
                        assembleModuleStructure.configure {
                            it.moduleArtifactDirs.add(gatherArtifacts.artifactOutputDir)
                        }
                    }
                }
            }
        }

        rootAssemble?.dependsOn(sign)
    }

    /**
     * The plugin utilizes two custom Configurations in order to designate those dependencies the the project requires
     * for the module to function correctly when loaded into the Ignition Platform.  These resolvable Configurations
     * extend from the 'api' and 'implementation' configurations added by the _java-library-plugin_.   though it should
     *
     * Currently, there is no functional difference in these configurations, but they are provided to follow best
     * practices in declaring dependencies.  While there are no current plans for Ignition to support Java modules or
     * restricted class visibility, such functionality could be supported in the future to make use of these different
     * configuration types.
     */
    private fun createConfigurations(p: Project): List<Configuration> {
        val apiConf: Configuration? = p.configurations.getByName(JavaPlugin.API_CONFIGURATION_NAME)
        val implementationConf: Configuration? = p.configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)

        val modlImplementation = p.configurations.create(MODULE_IMPLEMENTATION_CONFIGURATION) {
            it.isCanBeResolved = true
        }
        implementationConf?.extendsFrom(modlImplementation)

        val modlApi = p.configurations.create(MODULE_API_CONFIGURATION) {
            it.isCanBeResolved = true
        }
        apiConf?.extendsFrom(modlApi)

        return listOf(modlImplementation, modlApi)
    }

    /**
     * Registers tasks that depend on/utilize the output of java/jvm targetted compilation.
     */
    private fun createJavaTasks(
        p: Project,
        rootModuleProject: Project,
        settings: ModuleSettings
    ): List<TaskProvider<out Task>> {

        p.logger.info("Setting up Java tasks on ${p.path}")

        val assemble = p.tasks.findByName("assemble")

        val collectModlDependencies: TaskProvider<CollectModlDependencies> = p.tasks.register(
            CollectModlDependencies.ID,
            CollectModlDependencies::class.java
        ) {
            it.dependsOn(p.tasks.findByName("jar"))
            assemble?.dependsOn(it)
            it.projectScopes.set(settings.projectScopes.get())
            it.moduleVersion.set(settings.moduleVersion.get())
        }

        val tasks = listOf(collectModlDependencies)
        assemble?.dependsOn(tasks)

        rootModuleProject.tasks.findByName("assemble")?.dependsOn(assemble)
        rootModuleProject.tasks.findByName(AssembleModuleAssets.ID)?.dependsOn(collectModlDependencies)

        return tasks
    }

    /**
     * Adds the inductive automation artifact repositories to the plugin so that SDK artifacts are resolvable without
     * having to manually specify them in the build.gradle file.
     */
    private fun addInductiveAutoRepos(project: Project) {
        if (project.properties[APPLY_IA_REPOSITORY_FLAG] != "false") {
            project.allprojects.forEach { p: Project ->
                p.repositories.maven { m: MavenArtifactRepository ->
                    m.url = project.uri("https://nexus.inductiveautomation.com/repository/public")
                }
            }
        }
    }
}
