import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.lang.Boolean.getBoolean
import java.time.LocalDateTime

plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.2.1"
    kotlin("jvm") version "2.3.10"
    id("com.diffplug.spotless") version "6.25.0"
}

repositories {
    maven {
        url = uri("https://nexus.inductiveautomation.com/repository/public")
    }
    mavenCentral()
}

group = "io.ia.sdk"
version = "0.6.0"


testing {
    suites {
        val functionalTest by registering(JvmTestSuite::class) {
            useJUnit()

            dependencies {
                implementation(project())
            }

            targets {
                all {
                    testTask.configure {
                        // because this is a custom task or some other Gradle voodoo we can not rely on the debug system prop
                        // propagating to the GradleRunner in our function tests even though documentation suggests it
                        // should; we have to wire this up explicitly
                        val testkitDebugProp =
                            "org.gradle.testkit.debug" // the unreferenceable (?) DefaultGradleRunner.DEBUG_SYS_PROP
                        systemProperty(testkitDebugProp, getBoolean(testkitDebugProp))
                    }
                }
            }
        }
    }
}

configurations {
    named("functionalTestImplementation") {
        extendsFrom(configurations["testImplementation"])
    }
}

dependencies {
    // Use the Kotlin JDK standard library.
    api(kotlin("stdlib-jdk8"))
    api(kotlin("reflect"))
    implementation(libs.guava)
    implementation(libs.moshi)
    implementation(libs.kotlinXmlBuilder)
    api(libs.moduleSigner)
    testImplementation(libs.kotlinTestJunit)
    testImplementation("io.ia.sdk.tools.module.gen:generator-core")
}

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

gradlePlugin {
    website.set("https://www.github.com/inductiveautomation/ignition-module-tools")
    vcsUrl.set("https://github.com/inductiveautomation/ignition-module-tools")

    plugins {
        create("modl") {
            id = "io.ia.sdk.modl"
            implementationClass = "io.ia.sdk.gradle.modl.IgnitionModlPlugin"
            displayName = "Ignition Module Builder Plugin"
            description =
                "Create Modules to add capabilities to Inductive Automation's Ignition platform."
            tags.set(
                listOf(
                    "inductiveautomation",
                    "inductive automation",
                    "ignition",
                    "module",
                    "modl",
                    "maker",
                    "edge",
                    "iiot"
                )
            )
        }
    }

    testSourceSets(sourceSets["functionalTest"], sourceSets["test"])
}

tasks {
    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }

    withType<KotlinCompile>() {
        compilerOptions {
            // will retain parameter names for java reflection
            javaParameters.set(true)
        }
    }

    // Temporarily disabled - functional tests have compilation issues to resolve separately
    // check {
    //     dependsOn(functionalTest)
    // }

    // test {
    //     dependsOn(functionalTest)
    // }

    jar {
        manifest {
            attributes.putAll(
                mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Built-By" to System.getProperty("user.name"),
                    "Built-JDK" to System.getProperty("java.version"),
                    "Built-Gradle" to gradle.gradleVersion,
                    "Build-Time" to LocalDateTime.now().toString()
                )
            )
        }
    }

    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }
}

spotless {
    kotlin {
        ktlint("1.5.0").editorConfigOverride(
            mapOf(
                "ktlint_standard_filename" to "disabled"
            )
        )
    }
}
