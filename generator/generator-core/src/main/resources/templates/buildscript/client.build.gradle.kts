plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(11))
    }
}

dependencies {
    //<CLIENT_DEPENDENCIES>
    // add client scoped dependencies here
}
