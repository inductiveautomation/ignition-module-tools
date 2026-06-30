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
    mavenCentral()
}

java {
    withJavadocJar()
    withSourcesJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks {
    withType<KotlinCompile> {
        compilerOptions {
            javaParameters.set(true)
        }
    }

    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }

    named<KotlinCompile>("compileTestKotlin") {
        // don't try compiling resources that somehow end up in the test compilation path when we add the integration
        // test suite
        exclude("**/resources/**/*.kts")
    }
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform(kotlin("bom")))
    // Use SLF4J api for logging, logger implementation to be provided by lib consumer
    api(libs.slf4jApi)

    // Use the Kotlin test library.
    // testImplementation(libs.bundles.kotlinTest)
    testImplementation(libs.coroutinesCore)
    testImplementation(kotlin("test-junit"))

    // support logging in tests
    testImplementation(libs.slf4jApi)
    testImplementation(libs.slf4jSimple)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class)

        val integrationTest by registering(JvmTestSuite::class) {
            useJUnit()
            dependencies {
                implementation(project())
                implementation(libs.kotlinTestJunit)
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                        testClassesDirs =
                            sourceSets.named("integrationTest").get().output.classesDirs
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
        ktlint("1.5.0").editorConfigOverride(mapOf("ktlint_standard_filename" to "disabled"))

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

    val PUBLISHING_KEY =
        "ignitionModuleGen.maven.repo.${if ("$version".contains("-SNAPSHOT")) "snapshot" else "release"}"

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
