# Ignition Module Generator CLI

A command-line interface for creating skeleton projects for Inductive Automation's Ignition platform.


# How to Use

Download the latest release for your platform from the releases tab, and simply run it.  Follow the prompts to
 customize the module's initial setup.


# Building

This project uses Gradle for build tooling and includes the gradle wrapper.  To build, simply execute `./gradlew
 build` on posix machines, or `gradle.bat build` on Windows.  The wrapper will download all necessary dependencies
  from the primary gradle.org server and then execute the build using the correct version of the grade build tool.

### Requirements

   - *GraalVM JDK 25* should resolve automatically via the [Gradle Java Toolchain](https://docs.gradle.org/current/userguide/toolchains.html) api.  To explicitly specify JDK available to gradle, you have a few options: rely on the `JAVA_HOME` environmental variable, set `org.gradle.java.home=/path/to/jdk/home` as a commandline flag (`-Dorg.gradle.java.home=/path`), or specify the `org.gradle.java.home` in a [gradle.properties](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties) file.


### How To


To see a list of all tasks available, run `./gradlew tasks` or if on Windows, `gradle.bat tasks`.


# Native Images

The CLI project uses the [GraalVM Native Build Tools](https://graalvm.github.io/native-build-tools/) Gradle plugin to
generate a native binary executable for the platform running the build. Mac and Linux users can simply run
`./gradlew nativeCompile` in the cli project's directory, and a native binary will be created in the
`build/native/nativeCompile` directory. While module authors will require a JDK to build modules, this binary will
execute and generate module projects without any installed JVM/JRE.

See the official [GraalVM Native Image docs](https://www.graalvm.org/latest/reference-manual/native-image/) for details
on environmental prerequisites and platform-specific setup.


## Questions?  Feedback?  Want to Contribute?

Open an Issue.
