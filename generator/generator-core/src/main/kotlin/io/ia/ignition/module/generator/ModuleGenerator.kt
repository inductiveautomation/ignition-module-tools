package io.ia.ignition.module.generator

import io.ia.ignition.module.generator.api.DefaultDependencies
import io.ia.ignition.module.generator.api.DefaultDependencies.toDependencyFormat
import io.ia.ignition.module.generator.api.GeneratorConfig
import io.ia.ignition.module.generator.api.GradleDsl
import io.ia.ignition.module.generator.api.ProjectScope
import io.ia.ignition.module.generator.data.ModuleGeneratorContext
import io.ia.ignition.module.generator.data.ValidationResult
import io.ia.ignition.module.generator.data.validateModuleName
import io.ia.ignition.module.generator.data.validatePackagePath
import io.ia.ignition.module.generator.data.validateParentDirPath
import io.ia.ignition.module.generator.data.validateScope
import io.ia.ignition.module.generator.error.IllegalConfigurationException
import io.ia.ignition.module.generator.util.appendFromResource
import io.ia.ignition.module.generator.util.buildSubProjectSettings
import io.ia.ignition.module.generator.util.copyFromResource
import io.ia.ignition.module.generator.util.createAndFillFromResource
import io.ia.ignition.module.generator.util.createSourceDirs
import io.ia.ignition.module.generator.util.createSubProject
import io.ia.ignition.module.generator.util.replacePlaceholders
import io.ia.ignition.module.generator.util.writeHookFile
import org.slf4j.LoggerFactory
import java.nio.file.Path

object ModuleGenerator {
    val logger = LoggerFactory.getLogger("ModuleGenerator")

    /**
     * Generates a basic module structure, and populates stub files appropriately based on the path, name, scope and
     * package path provided.
     * @param configuration the configuration from which to derive the module structure, names, etc.  See
     * @throws IllegalArgumentException if configuration does not exist or validation fails
     * @throws Exception if module structure fails to be assembled
     */
    @JvmStatic
    @Throws(IllegalConfigurationException::class, Exception::class)
    fun generate(configuration: GeneratorConfig?): Path {
        when (configuration) {
            null -> throw IllegalArgumentException("Configuration does not exist")
            else -> {
                validate(configuration)
                return assembleModule(configuration)
            }
        }
    }

    /**
     * Validates the generated config that will be used to populate the module structure.  Done early to collect all
     * knowable validation errors in an atomic manner, but also to avoid doing any work we may need to cleanup due to
     * runtime errors.
     *
     * @param config the configuration from which to derive the module structure, names, etc. See [GeneratorConfig]
     * @throws IllegalStateException if configuration does not exist or validation fails
     */
    @Throws(IllegalStateException::class)
    fun validate(config: GeneratorConfig) {
        logger.debug("Validating configuration values...")

        val errors: List<ValidationResult> = listOf(
            validateModuleName(config.moduleName),
            validatePackagePath(config.packageName),
            validateParentDirPath(config.parentDir),
            validateScope(config.scopes)
        ).filter { !it.validated }

        if (errors.isNotEmpty()) {
            throw IllegalStateException("Validation failed $errors")
        }
    }

    /**
     * The main implementation for building the module structure based on the validated config
     * @param config the config from which to derive the module structure, names, etc. See [GeneratorConfig]
     * @throws Exception if module structure fails to be assembled
     */
    @Throws(Exception::class)
    private fun assembleModule(config: GeneratorConfig): Path {
        val context = ModuleGeneratorContext(config)
        val scopes = ProjectScope.scopesFromShorthand(config.scopes)

        if (context.isSingleDirProject()) {
            logger.info("Creating single directory module project...")
            val scope = scopes[0]
            val spConfig = buildSubProjectSettings(context, scope)
            val projectLanguage = config.projectLanguage
            val hookDir = createSourceDirs(spConfig.subprojectDir, spConfig.packagePath, projectLanguage)
            writeHookFile(hookDir, context, scope)
        } else {
            scopes.forEach { createSubProject(context, buildSubProjectSettings(context, it)) }
            if (scopes.size > 1) {
                createSubProject(context, buildSubProjectSettings(context, ProjectScope.COMMON))
            }
        }

        writeSettingsFile(context)
        writeRootBuildScript(context)
        writeGradleWrapperResources(context)

        return context.getRootDirectory()
    }

    /**
     * Writes the gradle wrapper, allowing the user to build the module without gradle installed.
     */
    private fun writeGradleWrapperResources(context: ModuleGeneratorContext) {
        val rootDir = context.getRootDirectory()
        val gradleVersion = context.config.gradleWrapperVersion
        val gradleResourcePath = "gradle/wrapper/${gradleVersion.replace(".", "_")}"

        val sourceWrapperJar = "$gradleResourcePath/gradle-wrapper.jar"
        val sourceWrapperProps = "$gradleResourcePath/gradle-wrapper.properties"
        val sourceBatchScript = "$gradleResourcePath/scripts/gradlew.bat"
        val sourceScript = "$gradleResourcePath/scripts/gradlew"

        val targetWrapperParentDir = rootDir.resolve("gradle/wrapper")
        val wrapperJarTarget = targetWrapperParentDir.resolve("gradle-wrapper.jar")
        val wrapperPropsTarget = targetWrapperParentDir.resolve("gradle-wrapper.properties")
        wrapperPropsTarget.createAndFillFromResource(sourceWrapperProps)

        val newBatchFile = rootDir.resolve("gradlew.bat")
        val newScriptFile = rootDir.resolve("gradlew")

        newBatchFile.createAndFillFromResource(sourceBatchScript)
        newScriptFile.createAndFillFromResource(sourceScript)

        wrapperPropsTarget.toFile().setReadable(true)
        wrapperJarTarget.toFile().setReadable(true)
        newBatchFile.toFile().setExecutable(true)
        newBatchFile.toFile().setReadable(true)
        newScriptFile.toFile().setExecutable(true)
        newScriptFile.toFile().setReadable(true)

        wrapperJarTarget.copyFromResource("$sourceWrapperJar")
    }

    /**
     * Writes the 'root' build script, which is the script containing the `ignitionModule` config extension object,
     * and applies the module plugin
     * @return root buildscript [Path]
     */
    private fun writeRootBuildScript(context: ModuleGeneratorContext): Path {
        val rootDir = context.getRootDirectory()
        val rootBuildScript = rootDir.resolve(context.getBuildScriptFilename())
        val templateResource = "templates/buildscript/root.${context.getBuildScriptFilename()}"
        val configurationResource = when (context.config.buildDsl) {
            GradleDsl.KOTLIN -> "templates/config/modlPluginConfig.kts"
            GradleDsl.GROOVY -> "templates/config/modlPluginConfig.groovy"
        }

        rootBuildScript.copyFromResource(templateResource)
            .appendFromResource(configurationResource)
            .replacePlaceholders(context.getTemplateReplacements())

        // also need to add dependencies to 'single dir project' root buildscripts
        if (context.isSingleDirProject()) {
            val scopeString = context.config.scopes
            val scopes = ProjectScope.scopesFromShorthand(scopeString)
            // should only be one scope in a single dir project
            val projectScope = if (scopes.size > 1) {
                throw Exception("A single directory project can only have one scope, but was configured with $scopeString")
            } else scopes.first()

            val dependencies =
                DefaultDependencies.ARTIFACTS[projectScope]?.toDependencyFormat(context.config.buildDsl) ?: ""

            rootBuildScript.toFile().appendText(
                """
                |dependencies {
                |    $dependencies
                |}
                """.trimMargin()
            )
        }

        return rootBuildScript
    }

    /**
     * Writes the root `settings.gradle` or `settings.gradle.kts`, populating it with the appropriate project name and
     * `include` entries according to the configuration provided to the generator.
     */
    private fun writeSettingsFile(context: ModuleGeneratorContext) {
        val replacements = context.getTemplateReplacements()
        val settingsFile = context.getRootDirectory().resolve(context.getSettingFileName())
        logger.debug("Populating settings file $settingsFile template with values $replacements")

        // write settings.gradle
        settingsFile.createAndFillFromResource(
            "templates/${context.settingsFilename()}",
            context.getTemplateReplacements()
        )
    }
}
