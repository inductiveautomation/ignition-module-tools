plugins {
    base
    kotlin("jvm") version "2.3.10" apply false
    id("org.jetbrains.dokka") version "2.1.0" apply false
    id("com.diffplug.spotless") version "8.3.0" apply false
}

allprojects {
    project.version = "1.1.0"
}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }
}

val runCli by tasks.registering {
    dependsOn(":generator-cli:run")
}
