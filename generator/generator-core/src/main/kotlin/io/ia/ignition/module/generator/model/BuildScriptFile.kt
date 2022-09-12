package io.ia.ignition.module.generator.model

import io.ia.ignition.module.generator.api.BuildFile
import io.ia.ignition.module.generator.api.GeneratorConfig
import io.ia.ignition.module.generator.api.GradleDsl
import io.ia.ignition.module.generator.api.ProjectScope

class BuildScriptFile(val type: GradleDsl, val projectScope: ProjectScope, config: GeneratorConfig): BuildFile {
    override fun getLocalFilePath(): String {
        // return when ()
        return ""
    }

    override fun getScope(): ProjectScope {
        return projectScope
    }

    override fun renderContents(): String {
        TODO("Not yet implemented")
    }
}
