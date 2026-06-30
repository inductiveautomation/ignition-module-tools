import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    kotlin("jvm")
    // kapt annotation processor plugin, for picocli/graal annotations
    kotlin("kapt")
    // Apply the application plugin to add support for building a CLI application.
    id("org.graalvm.buildtools.native") version "0.10.4"
    id("com.gradleup.shadow") version "9.0.0-beta12"
    id("com.diffplug.spotless")
}

repositories {
    mavenCentral()
}

java {
    withJavadocJar()
    withSourcesJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

group = "io.ia.sdk.tools.module.gen"

dependencies {
    // Align versions of all Kotlin components
    implementation(platform(kotlin("bom")))
    implementation(projects.generatorCore)
    // Use the Kotlin JDK 8 standard library.
    implementation(libs.picoCli)
    implementation(libs.slf4jSimple)
    // kapt used for annotation processing when creating the native image
    kapt(libs.picoCliCodegen)
    compileOnly(libs.slf4jApi)
    // Use the Kotlin test library.
    testImplementation(kotlin("test-junit"))
}

val APP_MAIN_CLASS = "io.ia.sdk.tools.module.cli.ModuleGeneratorCli"

application {
    // Define the main class for the application
    this.mainClass.set(APP_MAIN_CLASS)
}

spotless {
    kotlin {
        // Optional user arguments can be set as such:
        ktlint().editorConfigOverride(
            mapOf(
                "indent_size" to "4",
                "continuation_indent_size" to "4"
            )
        )
    }
}


val binaryName = "ignition-module-gen"

graalvmNative {
    // This is broken basically: https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html#configuration-toolchains
    toolchainDetection.set(false)
    binaries {
        named("main") {
            imageName.set(binaryName)
            mainClass.set(APP_MAIN_CLASS)
            fallback.set(false)
            configurationFileDirectories.setFrom(layout.projectDirectory.file("src/main/resources"))
        }
    }
}

tasks {
    withType<KotlinCompile> {
        compilerOptions {
            javaParameters.set(true)
        }
    }

    nativeCompile {
        dependsOn(build)
    }
    named<JavaExec>("run") {
        standardInput = System.`in`
    }
    named<JavaExec>("runShadow") {
        standardInput = System.`in`
    }
}
