
pluginManagement {
    // includeBuild("../gradle-module-plugin")
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "ignition-module-generator"

// includeBuild("../gradle-module-plugin")

include(
    ":",
    ":generator-core",
    ":generator-cli"
)
