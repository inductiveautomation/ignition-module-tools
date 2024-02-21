package io.ia.sdk.gradle.modl.task

import com.inductiveautomation.ignitionsdk.ModuleSigner
import io.ia.sdk.gradle.modl.PLUGIN_TASK_GROUP
import io.ia.sdk.gradle.modl.api.Constants
import io.ia.sdk.gradle.modl.api.Constants.ALIAS_FLAG
import io.ia.sdk.gradle.modl.api.Constants.CERT_FILE_FLAG
import io.ia.sdk.gradle.modl.api.Constants.CERT_PW_FLAG
import io.ia.sdk.gradle.modl.api.Constants.KEYSTORE_FILE_FLAG
import io.ia.sdk.gradle.modl.api.Constants.KEYSTORE_PW_FLAG
import io.ia.sdk.gradle.modl.api.Constants.PKCS11_CFG_FILE_FLAG
import io.ia.sdk.gradle.modl.api.Constants.SIGNING_PROPERTIES
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import javax.inject.Inject

/**
 * Signs the module file, using credentials provided by the task running.
 */
open class SignModule @Inject constructor(_providers: ProviderFactory, _objects: ObjectFactory) : DefaultTask() {
    companion object {
        const val ID = "signModule"
        private const val SKIP = "<SKIP_SIGNING_ENABLED>" // placeholder prop value for skipModuleSigning
        private const val PKCS11_KS_TYPE = "PKCS11"
    }

    init {
        this.group = PLUGIN_TASK_GROUP
        this.description = "Signs the module using certificates/secrets specified via property file or property flag."
    }

    // the unsigned .modl file
    @get:InputFile
    val unsigned: RegularFileProperty = _objects.fileProperty()

    @get:Input
    val skipSigning: Property<Boolean> = _objects.property(Boolean::class.java).convention(false)

    // the signed modl file
    @get:OutputFile
    val signed: Provider<RegularFile> = unsigned.map {
        val unsignedFileName = it.asFile.name
        val signedName = unsignedFileName.replace(".unsigned", "")
        project.layout.buildDirectory.file(signedName).get()
    }

    @get:Input
    @get:Optional
    val keystorePath: Property<String> =
        _objects.property(String::class.java).convention(
            _providers.provider {
                val propKey =
                    Constants.SIGNING_PROPERTIES[KEYSTORE_FILE_FLAG] as String

                if (skipSigning.get()) SKIP
                else propFromProjectProps(propKey) // can be null
            }
        )

    @Option(
        option = KEYSTORE_FILE_FLAG,
        description =
        "Path to the keystore used for signing.  Resolves in the same manner as gradle's project.file('<path>')"
    )
    fun setKeystorePath(path: String) {
        keystorePath.set(path)
    }

    @get:Input
    @get:Optional
    val pkcs11CfgPath: Property<String> =
        _objects.property(String::class.java).convention(
            _providers.provider {
                val propKey =
                    Constants.SIGNING_PROPERTIES[PKCS11_CFG_FILE_FLAG] as String

                if (skipSigning.get()) SKIP
                else propFromProjectProps(propKey) // can be null
            }
        )

    @Option(
        option = PKCS11_CFG_FILE_FLAG,
        description =
        "Path PKCS#11 HSM config file used for signing. " +
            "Resolves in the same manner as gradle's project.file('<path>')"
    )
    fun setPKCS11Path(path: String) {
        pkcs11CfgPath.set(path)
    }

    /**
     * If set to true, resolving relative path signing asset files will first check for relative to the module
     * root (where the module plugin is declared), and if not found, will also try to resolve relative to the
     * root project.  This is useful if you have multiple modules you build in the same gradle super project.
     *
     * If the module root IS the gradle root project, then this has no change on behavior.
     */
    @get:Input
    val allowMultiprojectFileResolution: Property<Boolean> = _objects.property(Boolean::class.java).convention(true)

    @get:InputFile
    @get:Optional
    val keystore: Provider<File> = keystorePath.zip(allowMultiprojectFileResolution) { path, allow ->
        var target = project.file(path)
        if (!target.exists() && allow && project != project.rootProject) {
            logger.info("Failed to resolve cert file at $target, attempting root project resolution.")
            target = project.rootProject.file(path)
        }
        target
    }

    @get:InputFile
    @get:Optional
    val pkcs11Cfg: Provider<File> =
        pkcs11CfgPath.zip(allowMultiprojectFileResolution) { path, allow ->
            var target = project.file(path)
            if (!target.exists() && allow && project != project.rootProject) {
                logger.info(
                    "Failed to resolve PKCS#11 config file at $target, " +
                        "attempting root project resolution."
                )
                target = project.rootProject.file(path)
            }
            target
        }

    @get:Input
    val keystorePw: Property<String> = _objects.property(String::class.java).convention(
        _providers.provider {
            if (skipSigning.get()) SKIP else propOrLogError(KEYSTORE_PW_FLAG, "keystore password")
        }
    )

    @Option(option = KEYSTORE_PW_FLAG, description = "The password for the keystore used in signing.")
    fun setKeystorePw(pw: String) {
        keystorePw.set(pw)
    }

    // path to the certificate file
    @get:Input
    val certFilePath: Property<String> = _objects.property(String::class.java).convention(
        _providers.provider {
            if (skipSigning.get()) SKIP else propOrLogError(CERT_FILE_FLAG, "certificate file location")
        }
    )

    @Option(option = CERT_FILE_FLAG, description = "Path to the certificate file used for signing modules")
    fun setCertFilePath(path: String) {
        certFilePath.set(path)
    }

    /**
     *  The certificate file, as derived from the certFilePath, if populated.  Otherwise will first look for properties
     *  in a specified property file, and if that fails, will try to resolve project properties, as would occur
     *  if supplying a `-P` arg at the commandline, or if using gradle.properties files in default locations.
     */
    @get:InputFile
    val certFile: Provider<File> = certFilePath.zip(allowMultiprojectFileResolution) { cert, allow ->
        var target = project.file(cert)

        if (!target.exists() && allow && project.rootProject != project) {
            logger.info("Failed to resolve cert file at $target, attempting root project resolution.")

            target = project.rootProject.file(cert)
        }

        target
    }

    // the certificate alias to use for the signing
    @get:Input
    val alias: Property<String> = _objects.property(String::class.java).convention(
        _providers.provider {
            if (skipSigning.get()) SKIP else propOrLogError(ALIAS_FLAG, "certificate alias")
        }
    )

    @Option(option = ALIAS_FLAG, description = "Alias for the CA cert in the provided keystore")
    fun setAlias(a: String) {
        alias.set(a)
    }

    @get:Input
    val certPw: Property<String> = _objects.property(String::class.java).convention(
        _providers.provider {
            if (skipSigning.get()) SKIP else propOrLogError(CERT_PW_FLAG, "certificate password")
        }
    )

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun propOrLogError(flag: String, itemName: String): String {
        val propKey = Constants.SIGNING_PROPERTIES[flag] as String
        val propValue = propFromProjectProps(propKey)
        if (propValue == null) {
            logger.error(
                "Required $itemName not found.  Specify via flag '--$flag=<value>', or in gradle.properties" +
                    " file as '$propKey=<value>'"
            )
        }
        return propValue.toString()
    }

    private fun propFromProjectProps(propKey: String): String? =
        project.properties[propKey] as String?

    @Option(option = CERT_PW_FLAG, description = "The password for the certificate used in signing.")
    fun setCertPw(pw: String) {
        certPw.set(pw)
    }

    @Internal
    fun getKeyStore(): KeyStore {
        project.logger.debug("Resolving keystore...")

        // File-base keystore
        if (keystore.isPresent) {
            val keystoreFile = keystore.get()
            if (keystoreFile.exists()) {
                return if (keystoreFile.extension == "pfx") {
                    project.logger.debug("Found keyfile extension of '.pfx', using KeyStore instance type 'pkcs12'")
                    KeyStore.getInstance("pkcs12")
                } else {
                    project.logger.debug("Using KeyStore instance type 'jks'")
                    KeyStore.getInstance("jks")
                }
            } else {
                throw Exception("Signing key file ${keystoreFile.absolutePath} did not exist!")
            }
        }

        // PKCS#11 HSM (hardware key)-based keystore
        if (pkcs11Cfg.isPresent) {
            project.logger.debug(
                "PKCS#11 config specified, using KeyStore instance type 'PKCS11'"
            )
            val cfgFile = pkcs11Cfg.get()
            val cfgPath = cfgFile.absolutePath
            if (!cfgFile.exists()) {
                throw FileNotFoundException(
                    "PKCS#11 configuration file [$cfgPath] does not exist."
                )
            }

            val pvdr = Security.getProvider("SunPKCS11").configure(cfgPath)
            Security.addProvider(pvdr)
            return KeyStore.getInstance(PKCS11_KS_TYPE)
        }

        // Backstop, we don't know what kind of keystore we have
        throw Exception("Failed to resolve keystore!")
    }

    @TaskAction
    fun execute() {
        logger.info("Begin evaluating module signing settings...")
        val unsignedModule = unsigned.get().asFile

        if (!unsignedModule.exists()) {
            throw Exception("Signing failed, module file '${unsignedModule.absolutePath}' not found.")
        } else {
            logger.debug("Found unsigned module at ${unsignedModule.absolutePath}...")
        }

        // If both file- and PKCS#11-based keystores are specified, fail.
        // Mechanically that means these properties are both 1) set, 2) set
        // to something other than the SKIP value, and 3) not explicitly set
        // to null via property convention backstop logic.
        val mutexPaths = listOf(keystorePath, pkcs11CfgPath)
        if (
            mutexPaths.map(Property<String>::getOrNull)
                .none { it in listOf(SKIP, null) }
        ) {
            throw InvalidUserDataException(
                "Signing failed, specify '--$KEYSTORE_FILE_FLAG' flag/" +
                    "'${SIGNING_PROPERTIES[KEYSTORE_FILE_FLAG]}' property in " +
                    "gradle.properties or '--$PKCS11_CFG_FILE_FLAG' flag/" +
                    "'${SIGNING_PROPERTIES[PKCS11_CFG_FILE_FLAG]}' property in " +
                    "gradle.properties but not both."
            )
        }

        // Converseley if neither flavor of keystore is specified, also fail.
        if (mutexPaths.all { it.getOrNull() == null }) {
            throw InvalidUserDataException(
                "Signing failed, specify '--$KEYSTORE_FILE_FLAG' flag/" +
                    "'${SIGNING_PROPERTIES[KEYSTORE_FILE_FLAG]}' property in " +
                    "gradle.properties or '--$PKCS11_CFG_FILE_FLAG' flag/" +
                    "'${SIGNING_PROPERTIES[PKCS11_CFG_FILE_FLAG]}' property in " +
                    "gradle.properties."
            )
        }

        logger.debug("Signed module will be named ${signed.get().asFile.absolutePath}")

        signModule(
            keystore.getOrNull(),
            pkcs11Cfg.getOrNull(),
            keystorePw.get(),
            certFile.get(),
            certPw.get(),
            alias.get(),
            unsignedModule,
            signed.get().asFile
        )
        logger.info("Module built and signed at ${signed.get().asFile.absolutePath}")
    }

    /**
     * Signs the module file
     */
    @Suppress("MemberVisibilityCanBePrivate")
    @Throws(IOException::class)
    protected fun signModule(
        keyStoreFile: File?,
        pkcs11CfgFile: File?,
        keystorePassword: String,
        cert: File,
        certPassword: String,
        certAlias: String,
        unsignedModule: File,
        outFile: File,
    ) {
        logger.debug(
            "Signing module with keystoreFile: ${keyStoreFile?.absolutePath}, " +
                "pkcs11CfgFile: ${pkcs11CfgFile?.absolutePath}, " +
                "keystorePassword: ${"*".repeat(20)}, " +
                "cert: ${cert.absolutePath}, " +
                "certPw: ${"*".repeat(20)}, " +
                "certAlias: $certAlias"
        )

        val keyStore: KeyStore = getKeyStore()
        loadKeyStore(keyStore, keystorePassword, keyStoreFile)

        val privateKey: PrivateKey = keyStore.getKey(certAlias, certPassword.toCharArray()) as PrivateKey

        ModuleSigner(privateKey, cert.inputStream())
            .signModule(PrintStream(OutputStream.nullOutputStream()), unsignedModule, outFile)
    }

    private fun loadKeyStore(ks: KeyStore, ksPwd: String, ksFile: File?) {
        ksFile?.inputStream()
            // if PKCS#11 HSM (hardware key) keystore, forgo the input stream
            ?.takeUnless { ks.type == PKCS11_KS_TYPE }
            .use { maybeStream ->
                ks.load(maybeStream, ksPwd.toCharArray())
            }
    }
}
