package io.ia.sdk.gradle.modl.task

import com.inductiveautomation.ignitionsdk.ModuleSigner
import io.ia.sdk.gradle.modl.PLUGIN_TASK_GROUP
import io.ia.sdk.gradle.modl.api.Constants
import io.ia.sdk.gradle.modl.api.Constants.ALIAS_FLAG
import io.ia.sdk.gradle.modl.api.Constants.CERT_FILE_FLAG
import io.ia.sdk.gradle.modl.api.Constants.CERT_PW_FLAG
import io.ia.sdk.gradle.modl.api.Constants.KEYSTORE_FILE_FLAG
import io.ia.sdk.gradle.modl.api.Constants.KEYSTORE_PW_FLAG
import org.gradle.api.DefaultTask
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
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream
import java.security.KeyStore
import java.security.interfaces.RSAPrivateKey
import javax.inject.Inject

/**
 * Signs the module file, using credentials provided by the task running.
 */
@Suppress("UnstableApiUsage")
open class SignModule @Inject constructor(_providers: ProviderFactory, _objects: ObjectFactory) : DefaultTask() {
    companion object {
        const val ID = "signModule"
    }

    init {
        this.group = PLUGIN_TASK_GROUP
        this.description = "Signs the module using certificates/secrets specified via property file or property flag."
    }

    // the unsigned .modl file
    @get:InputFile
    val unsigned: RegularFileProperty = _objects.fileProperty()

    // the signed modl file
    @get:OutputFile
    val signed: Provider<RegularFile> = unsigned.map {
        val unsignedFileName = it.asFile.name
        val signedName = unsignedFileName.replace(".unsigned", "")
        project.layout.buildDirectory.file(signedName).get()
    }

    @get:Input
    @get:Optional
    val keystorePath: Property<String> = _objects.property(String::class.java).convention(
        _providers.provider {
            propOrLogError(KEYSTORE_FILE_FLAG, "keystore file location")
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

    @get:InputFile
    val keystore: Provider<File> = keystorePath.map {
        project.file(it)
    }

    @get:Input
    val keystorePw: Property<String> = _objects.property(String::class.java).convention(
        _providers.provider {
            propOrLogError(KEYSTORE_PW_FLAG, "keystore password")
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
            propOrLogError(CERT_FILE_FLAG, "certificate file location")
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
    val certFile: Provider<File> = certFilePath.map {
        project.file(it)
    }

    // the certificate alias to use for the signing
    @get:Input
    val alias: Property<String> = _objects.property(String::class.java).convention(
        _providers.provider {
            propOrLogError(ALIAS_FLAG, "certificate alias")
        }
    )

    @Option(option = ALIAS_FLAG, description = "Alias for the CA cert in the provided keystore")
    fun setAlias(a: String) {
        alias.set(a)
    }

    @get:Input
    val certPw: Property<String> = _objects.property(String::class.java).convention(
        _providers.provider {
            propOrLogError(CERT_PW_FLAG, "certificate password")
        }
    )

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun propOrLogError(flag: String, itemName: String): String {
        val propKey = Constants.SIGNING_PROPERTIES[flag]
        val propValue = project.properties[propKey] as String?
        if (propValue == null) {
            logger.error(
                "Required $itemName not found.  Specify via flag '--$flag=<value>', or in gradle.properties" +
                    " file as '$propKey=<value>'"
            )
        }
        return propValue.toString()
    }

    @Option(option = CERT_PW_FLAG, description = "The password for the certificate used in signing.")
    fun setCertPw(pw: String) {
        certPw.set(pw)
    }

    @Internal
    fun getKeyStore(): KeyStore {
        project.logger.quiet("Getting keystore from key file...")
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

        logger.debug("Signed module will be named ${signed.get().asFile.absolutePath}")

        signModule(
            keystore.get(),
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
        keyStoreFile: File,
        keystorePassword: String,
        cert: File,
        certPassword: String,
        certAlias: String,
        unsignedModule: File,
        outFile: File
    ) {
        logger.debug(
            "Signing module with keystoreFile: ${keyStoreFile.absolutePath}, " +
                "keystorePassword: ${"*".repeat(keystorePassword.length)}, " +
                "cert: ${cert.absolutePath}, " +
                "certPw: ${"*".repeat(certPassword.length)}, " +
                "certAlias: $certAlias"
        )

        val keyStore: KeyStore = getKeyStore()
        keyStore.load(keyStoreFile.inputStream(), keystorePassword.toCharArray())

        val privateKey: RSAPrivateKey = keyStore.getKey(certAlias, certPassword.toCharArray()) as RSAPrivateKey

        ModuleSigner(privateKey, cert.inputStream())
            .signModule(PrintStream(OutputStream.nullOutputStream()), unsignedModule, outFile)
    }
}
