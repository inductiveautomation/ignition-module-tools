pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "ignition-module-generator"

include(
    ":",
    ":generator-core",
    ":generator-cli"
)

if (JavaVersion.current() != JavaVersion.VERSION_25) {
    throw GradleException(
        "This build must be run with java 25. Either set env var JAVA_HOME=<path to jdk 25 home>" +
            ", or add a gradle flag to your build command like '-Dorg.gradle.java.home=<path to " +
            "jdk 25 home>'"
    )
}
