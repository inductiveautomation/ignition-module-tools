package io.ia.sdk.gradle.modl.task

import com.inductiveautomation.ignitionsdk.ModuleSigner
import io.ia.sdk.gradle.modl.PLUGIN_TASK_GROUP
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.lang.Exception
import java.security.KeyStore
import java.security.interfaces.RSAPrivateKey
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * Signs the module file, using credentials provided by the task running.
 */
open class SignModule @javax.inject.Inject constructor(objects: ObjectFactory) : DefaultTask() {
    companion object {
        const val ID = "signModule"

        /**
         *   Property 'keys'.  Used as flags for commandline task options, or in the property file, prefixed with
         *   [PROPERTY_FILE_PROP] in the form `ignition.signing.keystoreFile=/path/to/file/my.keystore`.
         */
        const val KEY_FILE_PROP: String = "keystoreFile"
        const val KEYSTORE_PW_PROP: String = "keystorePassword"
        const val CERT_FILE_PROP: String = "certFile"
        const val CERT_PW_PROP: String = "certPassword"
        const val ALIAS_PROP: String = "certAlias"
        const val PROPERTY_FILE_PROP: String = "propertyFile"

        // Prepended to all property names when used in a standard property file (to avoid collisions).
        const val PROPERTY_PREFIX: String = "ignition.signing"
    }

    init {
        this.group = PLUGIN_TASK_GROUP
        this.description = "Signs the module using certificates/secrets specified via property file or property flag."
    }

    // the unsigned .modl file
    @InputFile
    val unsigned: RegularFileProperty = objects.fileProperty()

    // the signed modl file
    @OutputFile
    val signed: Provider<RegularFile> = unsigned.map {
        val unsignedFileName = it.asFile.name
        val signedName = unsignedFileName.replace(".unsigned", "")
        project.layout.buildDirectory.file(signedName).get()
    }

    // Commandline Input Options
    // path to the keystore file
    @Option(option = KEY_FILE_PROP, description = "Path to the keyfile used for signing modules")
    @Input
    @Optional
    val keyFilePath: Property<String> = objects.property(String::class.java)

    // path to the certificate file
    @Option(option = CERT_FILE_PROP, description = "Path to the certificate file used for signing modules")
    @Input
    @Optional
    val certFilePath: Property<String> = objects.property(String::class.java)

    // the certificate alias to use for the signing
    @Option(option = ALIAS_PROP, description = "Alias for the CA cert in the provided keystore")
    @Input
    @Optional
    val alias: Property<String> = objects.property(String::class.java)

    // the keystore File, as derived from the keyFilePath
    @Nested
    @Optional
    val keyFile: Provider<File> = keyFilePath.map { path -> project.file(path) }

    // the certificate file, as derived from the certFilePath
    @Nested
    @Optional
    val certFile: Provider<File> = certFilePath.map { project.file(it) }

    @Option(option = PROPERTY_FILE_PROP,
        description = "Property file with signing properties; optional alternative to providing commandline flags."
    )

    @Optional
    @InputFile
    val propertyFilePath: RegularFileProperty = objects.fileProperty()

    @Internal
    val loadedFileProps: Provider<Properties> = propertyFilePath.map { signPropFile ->
        val props = Properties()
        props.load(signPropFile.asFile.inputStream())
        props
    }

    @Internal
    fun getKeyStore(): KeyStore {
        project.logger.debug("Getting keystore from key file...")
        if (keyFile.isPresent) {
            val keys = keyFile.get()
            if (keys.exists()) {
                return if (keys.extension == "pfx") {
                    project.logger.debug("Found keyfile extension of '.pfx', using KeyStore instance type 'pkcs12'")
                    KeyStore.getInstance("pkcs12")
                } else {
                    project.logger.debug("Using KeyStore instance type 'jks'")
                    KeyStore.getInstance("jks")
                }
            } else {
                throw Exception("Signing key file ${keys.absolutePath} did not exist!")
            }
        }
        throw Exception("Failed to resolve '${keyFilePath.get()}', provided as `$KEY_FILE_PROP` property")
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

        if (loadedFileProps.isPresent) {
            logger.info("Using signing property file for cert and credential details...")
            val signProps = loadedFileProps.get()
            val keyStoreFile = project.file(signProps["$PROPERTY_PREFIX.$KEY_FILE_PROP"].toString())
            val cert = project.file(signProps["$PROPERTY_PREFIX.$KEY_FILE_PROP"].toString())
            val certAlias = signProps["$PROPERTY_PREFIX.$ALIAS_PROP"].toString()
            val keystorePw = signProps["$PROPERTY_PREFIX.$KEYSTORE_PW_PROP"].toString()
            val certPw = signProps["$PROPERTY_PREFIX.$CERT_PW_PROP"].toString()

            logger.debug("Signed module will be named ${signed.get().asFile.absolutePath}")

            signModule(keyStoreFile, keystorePw, cert, certPw, certAlias, unsignedModule, signed.get().asFile)
            logger.info("Module built and signed at ${signed.get().asFile.absolutePath}")
        } else {
            project.logger.error("Could not sign module, property file with signing credentials was not present!")
        }
    }

    /**
     * Signs the module file
     */
    fun signModule(
        keyStoreFile: File,
        keystorePassword: String,
        cert: File,
        certPassword: String,
        certAlias: String,
        unsignedModule: File,
        outFile: File
    ) {
        logger.debug("Signing module with keystoreFile: ${keyStoreFile.absolutePath}, " +
            "keystorePassword: ${"*".repeat(keystorePassword.length)}, " +
            "cert: ${cert.absolutePath}, " +
            "certPw: ${"*".repeat(certPassword.length)}, " +
            "certAlias: $certAlias")

        val keyStore: KeyStore = if (keyStoreFile.extension == "pfx") {
            logger.debug("using pkcs12 keystore type")
            KeyStore.getInstance("pkcs12")
        } else {
            logger.debug("using jks keystore type")
            KeyStore.getInstance("jks")
        }

        keyStore.load(keyStoreFile.inputStream(), keystorePassword.toCharArray())

        val privateKey: RSAPrivateKey = keyStore.getKey(certAlias, certPassword.toCharArray()) as RSAPrivateKey

        ModuleSigner(privateKey, cert.inputStream())
                .signModule(PrintStream(OutputStream.nullOutputStream()), unsignedModule, outFile)
    }
}
