buildscript {
    repositories {
        // axion release plugin is not available in gradle plugin portal, add maven central to resolve
        mavenCentral()
    }

    // required due to spotless and axion-release having conflicting versions of jgit on the path
    // see https://github.com/diffplug/spotless/issues/587
    // and https://github.com/allegro/axion-release-plugin/issues/343
    configurations.classpath {
        resolutionStrategy {
            force("org.eclipse.jgit:org.eclipse.jgit:5.7.0.202003110725-r")
        }
    }
}

plugins {
    base
    kotlin("jvm") version "1.5.21" apply false
    id("org.jetbrains.dokka") version "1.5.0" apply false
    id("com.diffplug.spotless") version "5.12.2" apply false
}

allprojects {
    project.version = "0.1.0-SNAPSHOT"
}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }
}

val runCli by tasks.registering(Exec::class) {
    dependsOn(":generator-cli:nativeImage")
    workingDir("$projectDir/module-generator/cli/build/graal")
    commandLine("./ignition-module-gen")
}
