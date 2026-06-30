includeBuild("gradle-module-plugin")
includeBuild("generator")

rootProject.name = "ignition-module-tools"


if (JavaVersion.current() != JavaVersion.VERSION_25) {
    throw GradleException(
        "This build must be run with java 25. Either set env var JAVA_HOME=<path to jdk 25 home>" +
                ", or add a gradle flag to your build command like '-Dorg.gradle.java.home=<path to " +
                "jdk 25 home>'"
    )
}
