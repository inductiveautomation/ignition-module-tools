package io.ia.sdk.gradle.modl.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_OK
import java.net.URL
import java.util.Base64

// TODO finish implementation and tests, then register task with the plugin
/**
 * Deploys the module to a running development gateway
 */
open class Deploy @javax.inject.Inject constructor(objects: ObjectFactory) : DefaultTask() {
    companion object {
        const val ID: String = "deploy"
        const val SERVLET_PATH: String = "/system/DeveloperModuleLoadingServlet"
    }

    init {
        this.mustRunAfter(":assemble")
    }

    @Option(option = "gateway", description = "Host url for the development gateway, including protocol and port.")
    @Input
    @Optional
    val gateway: Property<String> = objects.property(String::class.java)

    @Option(option = "module", description = "Signed module file to be deployed to the running dev gateway")
    @Input
    @Optional
    val signedModule: RegularFileProperty = objects.fileProperty()

    @get:Nested
    val targetUrl: String by lazy {
        "${gateway.get()}/$SERVLET_PATH"
    }

    @TaskAction
    fun deployToGateway() {
        if (!signedModule.isPresent) {
            logger.error(
                "Signed module property was not found.  Run `assemble` first, or specify module path with " +
                    "--module=/path/to/myfile.modl"
            )
            throw Exception("Module file not present!")
        }

        val module = signedModule.asFile.get()
        val b64 = Base64.getEncoder().encodeToString(module.readBytes())

        // connect to gw
        val connection = connect(URL(targetUrl))

        OutputStreamWriter(connection.outputStream).use { outstream ->
            outstream.write(b64)
            outstream.flush()
        }

        if (connection.responseCode != HTTP_OK || connection.responseCode != HTTP_CREATED) {
            DataInputStream(connection.inputStream).use { instream ->
                BufferedReader(instream.reader(Charsets.UTF_8)).use { reader ->
                    val output = reader.readLine()
                    logger.error("Error publishing module to gateway, $output")
                    throw Exception("Could not post module to gateway, $output")
                }
            }
        }
    }

    fun connect(url: URL): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 20000
        connection.doOutput = true
        connection.useCaches = false
        connection.setRequestProperty("Content-Type", "multipart/form-data")
        return connection
    }
}
