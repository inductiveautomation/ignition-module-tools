package io.ia.ignition.module.generator;

import java.nio.file.Path;

import io.ia.ignition.module.generator.api.GeneratorConfig;
import io.ia.ignition.module.generator.api.GeneratorConfigBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ModuleGeneratorJavaTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void generatorSucceedsInJavaWithValidConfig() throws Exception {
        final String scope = "G";
        final Path parentDir = tempFolder.newFolder().toPath();
        final String moduleName = "Custom Thing";
        final String packageName = "le.examp";

        GeneratorConfig config = new GeneratorConfigBuilder()
            .scopes(scope)
            .parentDir(parentDir)
            .packageName(packageName)
            .moduleName(moduleName)
            .build();

        Throwable t = null;

        // try {
            ModuleGenerator.generate(config);
        // } catch (Exception e) {
        //     t = e;
        // }

        assertNull(t);
    }

    @Test
    public void generatorFailsInJavaWithInvalidConfig() {
        Throwable t = null;
        try {
            ModuleGenerator.generate(null);
        } catch (Exception e) {
            t = e;
        }

        assertNotNull(t);
    }
}
