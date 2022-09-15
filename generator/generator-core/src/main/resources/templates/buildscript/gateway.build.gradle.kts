plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(11))
    }
}

dependencies {
    //<GATEWAY_DEPENDENCIES>
    // add gateway scoped dependencies here
}
