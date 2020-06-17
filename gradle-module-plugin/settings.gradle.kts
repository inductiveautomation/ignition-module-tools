rootProject.name = "gradle-module-plugin"

includeBuild("../module-generator/core") {
    dependencySubstitution {
        substitute(module("io.ia.sdk.tools.module.gen:core")).with(project(":"))
    }
}
