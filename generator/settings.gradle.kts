
pluginManagement {
    // includeBuild("../gradle-module-plugin")
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
// lets us use 'version catalogs': https://docs.gradle.org/7.0/release-notes.html
enableFeaturePreview("VERSION_CATALOGS")

rootProject.name = "ignition-module-generator"

// includeBuild("../gradle-module-plugin")

include(
    ":",
    ":generator-core",
    ":generator-cli"
)
