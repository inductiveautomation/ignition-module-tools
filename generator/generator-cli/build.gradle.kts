

plugins {
    application
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    kotlin("jvm")
    // kapt annotation processor plugin, for picocli/graal annotations
    kotlin("kapt")
    // Apply the application plugin to add support for building a CLI application.
    id("com.palantir.graal") version "0.7.1"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("com.diffplug.spotless")
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "io.ia.sdk.tools.module.gen"

val mainApplicationClass = "io.ia.sdk.tools.module.cli.ModuleGeneratorCli"

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(projects.generatorCore)
    // Use the Kotlin JDK 8 standard library.
    implementation(libs.picoCli)
    implementation(libs.slf4jSimple)

    // kapt used for annotation processing when creating the native image
    kapt("info.picocli:picocli-codegen:4.3.2")

    compileOnly(libs.slf4jApi)

    // Use the Kotlin test library.
    testImplementation(libs.bundles.kotlinTest)
}

val JVM_TARGET = "1.8"

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = JVM_TARGET
            // will retain parameter names for java reflection
            javaParameters = true
        }
    }

    compileTestKotlin {
        kotlinOptions {
            jvmTarget = JVM_TARGET
            javaParameters = true
        }
    }
}

application {
    // Define the main class for the application.
    mainClassName = mainApplicationClass
}

spotless {
    kotlin {
        // optionally takes a version
        ktlint()
        // Optional user arguments can be set as such:
        //  ktlint().userData(["indent_size": "4", "continuation_indent_size" : "8"])
    }
}


val reflectionConfigFile =
        "${buildDir}/resources/main/META-INF/native-image/${project.group}/${project.name}/reflect-config.json"
val resourceConfigFile = "src/main/resources/resource-config.json"

val binaryName = "ignition-module-gen"

graal {
    // javaVersion "8"
    // graalVersion "20.1.0"
    // mainClass mainApplicationClass
    // outputName binaryName
    // windowsVsVersion "2019"

    /*
     * Each option must be its own line-item, all will get added to the final options command args passed to the
     * substrate VM compiler
     */

    // tell graal/substrate to load resources that need to resolve via `Classloader.getSystemResource` style resolution
    // we define the patterns we want to support in the config json file found below, in accordance with
    // https://github.com/oracle/graal/blob/master/substratevm/OPTIONS.md
    // option "-H:ResourceConfigurationFiles=$resourceConfigFile"

    // don"t fallback to "jre-required" image if the full native image assembly fails
    // option("--no-fallback")

    // we don"t need this because we generate these dynamically at build-time
    // option "-H:ReflectionConfigurationFiles=$reflectionConfigFile"
}
tasks {
    nativeImage {
        dependsOn(build)
    }
}

val runNative by tasks.registering(Exec::class) {
    // workingDir("$buildDir/graal")
    // commandLine(binaryName)
    // dependsOn(nativeImage)
}

