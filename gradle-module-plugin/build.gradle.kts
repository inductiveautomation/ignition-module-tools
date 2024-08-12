import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.lang.Boolean.getBoolean
import java.time.LocalDateTime

plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.16.0"
    kotlin("jvm") version "1.7.10"
    id("com.diffplug.spotless") version "6.11.0"
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://nexus.inductiveautomation.com/repository/public")
    }
    mavenCentral()
}

group = "io.ia.sdk"
version = "0.4.0-SNAPSHOT"

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

    // because this is a custom task or some other Gradle voodoo we can not rely on the debug system prop
    // propagating to the GradleRunner in our function tests even though documentation suggests it
    // should; we have to wire this up explicitly
    val testkitDebugProp = "org.gradle.testkit.debug" // the unreferenceable (?) DefaultGradleRunner.DEBUG_SYS_PROP
    systemProperty(testkitDebugProp, getBoolean(testkitDebugProp))
}

dependencies {
    // Align versions of all Kotlin components
    // Use the Kotlin JDK standard library.
    implementation(kotlin("bom", KotlinCompilerVersion.VERSION))
    api(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
    api(kotlin("reflect", KotlinCompilerVersion.VERSION))
    implementation(libs.guava)
    implementation(libs.moshi)
    implementation(libs.kotlinXmlBuilder)
    api(libs.moduleSigner)
    testImplementation(libs.kotlinTestJunit)
    testImplementation("io.ia.sdk.tools.module.gen:generator-core")
}

repositories {
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

    withType<KotlinCompile>() {
        kotlinOptions {
            // will retain parameter names for java reflection
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
        ktlint("0.44.0").editorConfigOverride(mapOf(
            "ktlint_disabled_rules" to "filename"
        ))
    }
}
