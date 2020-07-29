package io.ia.ignition.module.generator.util

import io.ia.ignition.module.generator.api.GeneratorContext
import io.ia.ignition.module.generator.api.GradleDsl
import io.ia.ignition.module.generator.api.ProjectScope
import io.ia.ignition.module.generator.api.SupportedLanguage
import io.ia.ignition.module.generator.api.SupportedLanguage.JAVA
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("generatorUtils")

data class SubProjectSettings(
    val moduleRootDir: Path,
    val subprojectDir: Path,
    val packagePath: String,
    val buildscriptLanguage: GradleDsl,
    val projectLanguage: SupportedLanguage,
    val scope: ProjectScope,
    val dependencies: String = ""
) // optional dependencies injected into build file

fun buildSubProjectSettings(context: GeneratorContext, scope: ProjectScope): SubProjectSettings {
    val moduleRootDir = context.getRootDirectory()
    val config = context.config
    val subProjectDir = if (config.scopes.length == 1 && config.useRootProjectWhenSingleScope) {
        // the 'root' project is the 'subproject dir' in a single project setup
        moduleRootDir.toAbsolutePath()
    } else {
        moduleRootDir.resolve(scope.folderName).toAbsolutePath()
    }

    val packagePath = context.config.packageName.toPackagePath(scope)

    return SubProjectSettings(moduleRootDir, subProjectDir, packagePath, context.config.buildDsl,
            context.config.projectLanguage, scope)
}

/**
 * Creates a module subproject folder structure based on the subproject model, populating it with the appropriate
 * build.gradle file and hook files.  If the scope is 'common', instead of generating a module hook, will
 * generate an empty "ModuleName.java" file.
 *
 * @param context context object containing a validated raw GeneratorConfig
 * @param config subproject configuration object
 */
fun createSubProject(context: GeneratorContext, config: SubProjectSettings): Path {
    val projectLanguage = context.config.projectLanguage
    val hookDir = createSourceDirs(config.subprojectDir, config.packagePath, projectLanguage)
    writeHookFile(hookDir, context, config.scope)

    // write the buildscript for this subproject
    val buildScriptTemplateResource = "templates/buildscript/${config.scope.folderName}.${context.getBuildScriptFilename()}"
    val buildScript = config.subprojectDir.resolve(context.getBuildScriptFilename())

    buildScript.createAndFillFromResource(buildScriptTemplateResource, context.getTemplateReplacements())

    return config.subprojectDir
}

/**
 * Writes the stub hook file for the given scope, creating appropriate package directories and updating the templates
 * with appropriate class and package names.
 *
 * @param hookDir the ABSOLUTE path to where a module Hook would be created
 * @param context context containing validated config
 * @param scope Ignition scope this hook is being created for
 */
fun writeHookFile(hookDir: Path, context: GeneratorContext, scope: ProjectScope) {
    val hookExtension = context.config.projectLanguage.sourceCodeFileExtension()
    val hookClassFileName = "${context.getHookClassName(scope)}.$hookExtension"
    val hookFile = hookDir.toAbsolutePath().resolve(hookClassFileName)
    val resourcePath = "templates/${context.getHookResourcePath(scope)}"
    hookFile.createAndFillFromResource(resourcePath, context.getTemplateReplacements())
}

/**
 * Creates the source code directory structure for a conventional maven/gradle/java style project
 * @param parentDir the directory in which the source folders should be created, generally in a gradle project
 *                  directory
 * @param scopeTerminatedPackagePath the package path, with the final path element being the scope (if not a single
 *        scope/mono-project)
 * @param language the language intended to be used for this project.  Defaults to SupportedLanguage.JAVA
 * @return [Path] pointing to the src/main/package/name/scope source directory, where a hook file is commonly located
 */
fun createSourceDirs(parentDir: Path, scopeTerminatedPackagePath: String, language: SupportedLanguage = JAVA): Path {

    // make the main and test sourceset folders, e.g.  <parentDir>/src/main
    val srcMain = Paths.get(parentDir.toAbsolutePath().toString(), "src", "main")
    val srcTest = Paths.get(parentDir.toAbsolutePath().toString(), "src", "test")

    // append the package path and scope (scope derived from parent directory name), e.g. <sourcSet>/io/ia/thing/client
    val mainCode = srcMain.resolve(language.commonName()).resolve(scopeTerminatedPackagePath)
    val testCode = srcTest.resolve(language.commonName()).resolve(scopeTerminatedPackagePath)

    logger.debug("createSrcDirs().mainCode path is '$mainCode'")

    listOf(mainCode, testCode).forEach {
        if (!Files.exists(it)) {
            logger.debug("Creating: ${it.toAbsolutePath()}")
            Files.createDirectories(it)
        } else {
            logger.error("Could not create new source directory,dir already present at '${it.toFile().absolutePath}'")
        }
    }

    return mainCode
}
