import org.jetbrains.kotlin.ir.backend.js.compile
import java.time.LocalDateTime

plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.15.0"
    kotlin("jvm") version "1.5.21"
    kotlin("kapt") version "1.5.21"
    id("com.diffplug.spotless") version "5.14.2"
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://nexus.inductiveautomation.com/repository/public")
    }
    mavenCentral()
}

group = "io.ia.sdk"
version = "0.1.0-SNAPSHOT-15"

configurations {
    val functionalTestImplementation by registering {
        extendsFrom(configurations["testImplementation"])
    }
}

sourceSets {
    create("functionalTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output + configurations.testCompileClasspath
        runtimeClasspath += compileClasspath + configurations.testRuntimeClasspath
    }
}

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = sourceSets["functionalTest"].output
    classpath += sourceSets["functionalTest"].runtimeClasspath

    group = "verification"
    description = "Executes tests in the 'functionalTest' sourceset"
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    // Use the Kotlin JDK standard library.
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation(libs.guava)
    implementation(libs.moshi)
    kapt(libs.moshiCodegen)
    implementation(libs.kotlinXmlBuilder)
    api(libs.moduleSigner)
    testImplementation(libs.bundles.kotlinTest)
    testImplementation("io.ia.sdk.tools.module.gen:generator-core")
}

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        this.languageVersion.set(JavaLanguageVersion.of(11))
    }
}

gradlePlugin {
    plugins {
        create("modl") {
            id = "io.ia.sdk.modl"
            implementationClass = "io.ia.sdk.gradle.modl.IgnitionModlPlugin"
        }
    }

    testSourceSets(sourceSets["functionalTest"], sourceSets["test"])
}

pluginBundle {
    website = "https://www.github.com/inductiveautomation/ignition-module-tools"
    vcsUrl = "https://github.com/inductiveautomation/ignition-module-tools"
    description = "Create Modules to add capabilities to Inductive Automation's Ignition platform."
    plugins {
        named("modl") {
            displayName = "Ignition Module Builder Plugin"
            tags = setOf("inductiveautomation", "inductive automation", "ignition", "module", "modl", "maker", "iiot")
            version = "${project.version}"
        }
    }

    mavenCoordinates {
        // coordinates are automatically established as `gradle.plugin.${project.group}` when publishing to
        // gradle plugin portal, so no need to set this here.
        // groupId = project.getGroup()

        artifactId = "gradle-module-plugin"
        version = "${project.version}"
    }
}

tasks {
    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
            // will retain parameter names for java reflection
            javaParameters = true
        }
    }

    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "11"
            javaParameters = true
        }
    }

    check {
        dependsOn(functionalTest)
    }

    test {
        dependsOn(functionalTest)
    }

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
        ktlint("0.39.0").userData(mapOf("max_line_length" to "120"))
    }
}


