package io.ia.sdk.gradle.modl

import io.ia.sdk.gradle.modl.api.Constants.APPLY_IA_REPOSITORY_FLAG
import io.ia.sdk.gradle.modl.api.Constants.MODULE_DEPENDENCY_CONFIGURATION
import io.ia.sdk.gradle.modl.extension.EXTENSION_NAME
import io.ia.sdk.gradle.modl.extension.ModuleSettings
import io.ia.sdk.gradle.modl.task.AssembleModuleStructure
import io.ia.sdk.gradle.modl.task.Checksum
import io.ia.sdk.gradle.modl.task.CollectModlDependencies
import io.ia.sdk.gradle.modl.task.ModuleBuildReport
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
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

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
        project.logger.info("Applying module plugin to ${project.path}")

        if (project.plugins.hasPlugin(this.javaClass)) {
            throw Exception(
                "Project ${project.path} already has Ignition Module Plugin applied.  Plugin should only" +
                    " be applied to the 'parent' project, one plugin application per module created."
            )
        }

        this.appliedPluginProject = project

        // check for and set base plugin if not present, to establish lifecycle tasks/hooks we'll use
        if (!project.plugins.hasPlugin("base")) {
            project.plugins.apply("base")
        }

        // create the extension object used to configure the plugin in the user's buildscripts
        val settings = project.extensions.create(
            EXTENSION_NAME,
            ModuleSettings::class.java
        ).apply {
            this.moduleVersion.convention(project.version.toString())
        }

        project.afterEvaluate {
            if (settings.applyInductiveArtifactRepo.get()) {
                addInductiveAutoRepos(project)
            }
        }

        setupRootTasks(project, settings)
        project.allprojects.forEach { p: Project ->
            setupDependencyTasks(p, settings)
        }
    }

    /**
     * Creates and binds the appropriate tasks for projects (generally the module root project and its subprojects)
     * which generate artifacts (jars) for inclusion in a module.
     *
     * Projects may opt out of module contribution and the associated tasks by adding `excludeFromIgnitionModule` as a
     * project property.  This property only applies to the single project it is applied to.
     *
     * ```
     * // build.gradle
     * ext {
     *     excludeFromIgnitionModule=true
     * }
     * ```
     */
    private fun setupDependencyTasks(artifactContributor: Project, settings: ModuleSettings) {
        // allow projects to declare themselves exempt from the module plugin functionality by setting a property
        // in the build.gradle `ext { disableModulePlugin=true }
        if (artifactContributor.hasOptedOutOfModule()) {
            return
        }

        // apply the base plugin to establish a baseline lifecycle on all projects
        if (!artifactContributor.plugins.hasPlugin("base")) {
            artifactContributor.plugins.apply("base")
        }

        // ignition modules are built using configurations from gradle's 'java-library' plugin to allow proper
        // transitive dependency management, so we look for that plugin to be applied
        artifactContributor.plugins.withType(JavaPlugin::class.java) {
            createConfiguration(artifactContributor)
            createDependencyCollectionTasks(artifactContributor, this.appliedPluginProject, settings)
        }

        // assemble on root should depend on subproject assembles, but don't try to depend on self and create a
        // circular dependency
        if (this.appliedPluginProject != artifactContributor) {
            artifactContributor.logger.debug(
                "Setting ${this.appliedPluginProject.path}:assemble.dependsOn('${artifactContributor.path}:assemble')"
            )

            this.appliedPluginProject.tasks.findByName("assemble")?.dependsOn(
                "${artifactContributor.path}:assemble"
            )
        } else {
            artifactContributor.logger.debug("Skipping dependency from ${artifactContributor.path}:assemble on self...")
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
            AssembleModuleStructure.ID,
            AssembleModuleStructure::class.java
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
            zipTask.unsignedModule.set(
                settings.fileName.flatMap {
                    val fileName = if (it.endsWith(".modl")) {
                        it.replace(".modl", ".$UNSIGNED_EXTENSION")
                    } else {
                        "$it.$UNSIGNED_EXTENSION"
                    }
                    root.layout.buildDirectory.file(fileName)
                }
            )

            // need xml file written before we zip anything
            zipTask.dependsOn(writeModuleXml)
        }

        // task that signs the module, using [http://github.com/inductiveautomation/module-signer] to do so
        val sign = root.tasks.register(SignModule.ID, SignModule::class.java) { signTask ->
            signTask.unsigned.set(zip.flatMap { it.unsignedModule })
        }

        val checksum = root.tasks.register(Checksum.ID, Checksum::class.java) { checksum ->
            checksum.hashAlgorithm.set(settings.checksumAlgorithm)
            checksum.signedModl.set(sign.flatMap { it.signed })
            checksum.dependsOn(sign)
        }

        val buildReport = root.tasks.register(ModuleBuildReport.ID, ModuleBuildReport::class.java) { report ->
            report.metaInfo.putAll(settings.metaInfo)
            report.modlFile.set(sign.flatMap { it.signed })
            report.moduleId.set(settings.id)
            report.moduleName.set(settings.name)
            report.moduleVersion.set(settings.moduleVersion)
            report.checksumJson.set(checksum.flatMap { it.checksumJson })
            report.moduleDescription.set(settings.moduleDescription)
            report.dependsOn(sign, checksum)
        }

        root.allprojects.forEach { p ->
            p.logger.info("Evaluating `rootProject.allprojects` including ${p.path}")
            if (!p.hasOptedOutOfModule()) {
                // when a project has a task of the given name, apply appropriate task dependencies
                p.tasks.withType(CollectModlDependencies::class.java) { t ->
                    p.logger.info(
                        "Binding module aggregation tasks for '${root.path}:" +
                            "${CollectModlDependencies.ID}' to depend on outputs from '${p.path}'"
                    )

                    // bind artifact collection task output to xml writing task input
                    writeModuleXml.configure {
                        it.artifactManifests.add(t.manifestFile)
                    }

                    // bind artifact manifests to the build report for inclusion
                    buildReport.configure {
                        it.childManifests.put(p.path, t.manifestFile)
                    }

                    // bind artifact collection dir to the module content collection task input
                    assembleModuleStructure.configure {
                        it.moduleArtifactDirs.add(t.artifactOutputDir)
                    }
                }
            }
        }

        // root project can be a module artifact contributor, so we'll apply the tasks to root as well (may opt out)
        rootAssemble?.dependsOn(buildReport)
    }

    /**
     * The plugin utilizes two custom Configurations in order to designate those dependencies the project requires
     * for the module to function correctly when loaded into the Ignition Platform.  The resolvable `modlDependency`
     * Configuration is a consumable configuration whose artifact, as well as its transitive artifacts are extend from the 'api' and 'implementation' configurations added by the _java-library-plugin_.   though it should
     *
     * Currently, there is no functional difference in these configurations, but they are provided to follow best
     * practices in declaring dependencies.  While there are no current plans for Ignition to support Java modules or
     * restricted class visibility, such functionality could be supported in the future to make use of these different
     * configuration types.
     */
    private fun createConfiguration(p: Project): Configuration {
        val implementationConf: Configuration? =
            p.configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)

        val modlDependency = p.configurations.create(MODULE_DEPENDENCY_CONFIGURATION) {
            it.isCanBeResolved = true
            it.isTransitive = true
            it.isCanBeConsumed = true
            implementationConf?.extendsFrom(it)
        }

        return modlDependency
    }

    /**
     * Registers tasks that depend on/utilize the output of java/jvm targetted compilation.
     */
    private fun createDependencyCollectionTasks(
        p: Project,
        rootModuleProject: Project,
        settings: ModuleSettings
    ): List<TaskProvider<out Task>> {

        p.logger.info("Setting up Java tasks on ${p.path}")

        // version won't actually be readable on the root project until after the plugin has been applied, so we
        // hold off trying to read it until after evaluation.
        p.afterEvaluate {
            p.tasks.named("jar") {
                if (it is Jar) {
                    if (p.version.toString() == "unspecified") {
                        throw Exception("${p.path} had an undefined version. Define a version in its build script.")
                    }
                    it.archiveFileName.convention("${p.name}-${p.version}.jar")
                }
            }
        }

        val assemble = p.tasks.findByName("assemble")

        val collectModlDependencies: TaskProvider<CollectModlDependencies> = p.tasks.register(
            CollectModlDependencies.ID,
            CollectModlDependencies::class.java
        ) {
            it.dependsOn(p.tasks.named("jar"))
            assemble?.dependsOn(it)
            it.projectScopes.set(settings.projectScopes)
            it.moduleVersion.set(settings.moduleVersion)
        }

        val tasks = listOf(collectModlDependencies)
        assemble?.dependsOn(tasks)

        rootModuleProject.tasks.findByName(AssembleModuleStructure.ID)?.dependsOn(collectModlDependencies)

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
