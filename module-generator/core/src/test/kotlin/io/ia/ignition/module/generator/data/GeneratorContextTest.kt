package io.ia.ignition.module.generator.data

import io.ia.ignition.module.generator.api.GeneratorConfig
import io.ia.ignition.module.generator.api.GeneratorContext
import io.ia.ignition.module.generator.api.GradleDsl
import java.nio.file.Paths
import kotlin.test.assertNotNull
import org.junit.Test

class GeneratorContextTest {

    @Test
    fun `context created successfully from correct settings object`() {
        val settings = GeneratorConfig.ConfigBuilder()
                .settingsDSL(GradleDsl.GROOVY)
                .moduleName("Skynet Launcher")
                .packageName("bot.terminator.launcher")
                .parentDir(Paths.get(System.getProperty("user.home")))
                .scopes("G")
                .build()

        val context: GeneratorContext = ModuleGeneratorContext(settings)

        assertNotNull(context, "GeneratorContext should not have been null!")
    }
}
