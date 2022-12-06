import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.include
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `maven-publish`
    signing
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("com.diffplug.spotless")
}

group = "io.ia.sdk.tools.module.gen"

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    withJavadocJar()
    withSourcesJar()

    toolchain {
        this.languageVersion.set(JavaLanguageVersion.of(11))
    }
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(11))
    }
}


tasks {
    withType(KotlinCompile::class) {
        kotlinOptions {
            javaParameters = true
        }
    }

    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }

    named<KotlinCompile>("compileTestKotlin") {
        // don't try compiling resources that somehow end up in the test compilation path when we add the integration
        // test suite
        sourceSets {
            exclude("**/resources/**/*.groovy")
            exclude("**/resources/**/*.kts")
        }
    }
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    // Use SLF4J api for logging, logger implementation to be provided by lib consumer
    api(libs.slf4jApi)

    // Use the Kotlin test library.
    // testImplementation(libs.bundles.kotlinTest)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    testImplementation(kotlin("test-junit"))

    // support logging in tests
    testImplementation(libs.slf4jApi)
    testImplementation(libs.slf4jSimple)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class)

        val integrationTest by registering(JvmTestSuite::class) {
            // useKotlinTest()
            dependencies {
                implementation(project)
                implementation(libs.kotlinTestJunit)
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                        testClassesDirs = sourceSets.named("integrationTest").get().output.classesDirs
                    }
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}

spotless {
    kotlin {
        // optionally takes a version
        ktlint("0.44.0").editorConfigOverride(mapOf("ktlint_disabled_rules" to "filename"))

        targetExclude(
            "src/main/resources/templates/config/*.kts",
            "src/main/resources/templates/buildscript/*.build.gradle.kts",
            "src/main/resources/templates/settings.gradle.kts",
            "src/main/resources/templates/hook/*.kt"
        )
    }
}

// Artifact publishing configuration
publishing {
    publications {
        create<MavenPublication>("ignitionModuleGenerator") {
            from(components["java"])
            this.version = project.version.toString()
            this.groupId = project.group.toString()
            this.artifactId = project.name

        }
    }

    val PUBLISHING_KEY = "ignitionModuleGen.maven.repo.${if ("$version".contains("-SNAPSHOT")) "snapshot" else "release"}"

    repositories {
        maven {
            name = "${PUBLISHING_KEY}.name".load()
            url = uri("${PUBLISHING_KEY}.url".load())

            credentials {
                username = "${PUBLISHING_KEY}.username".load()
                password = "${PUBLISHING_KEY}.password".load()
            }
        }
    }
}


/**
 * Returns the gradle property value associated with the string, if present.  Otherwise, returns the string "null".
 *
 * The properties may be defined any way described in the corresponding Gradle Properties docs at
 * https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties
 *
 * Suggestion using either:
 *
 * 1. Providing them at task runtime via `-Psome.property.key=value`
 * 2. gradle.properties file located in your gradle home dir (default: `~/.gradle/gradle.properties`)
 */
fun String.load(): String {
    return project.rootProject.properties[this]?.toString() ?: "null"
}
