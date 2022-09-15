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

   - *JDK 11* should resolve automatically via the Gradle JavaToolchain api.  To specify JDK available to gradle, via
system `JAVA_HOME` variable, or `org.gradle.java.home =/path/to/jdk/home` as a commandline flag
(`-Dorg.gradle.java.home.=/path`), or in the gradle.properties file.

### How To


To see a list of all tasks available, run `./gradlew tasks` or if on Windows, `gradle.bat tasks`.


# Native Images

The CLI project uses [Palantir's Gradle Graal plugin](https://github.com/palantir/gradle-graal) to generate a native
  binary executable for the platform running the build.   Mac and Linux users can simply run `./gradlew :nativeImage
   ` in the cli project's directory, and a native binary will be created in the `module-generator/cli/build/graal
   `  directory.  While module authors will require a JDK to build modules, this binary will execute and generate
    module projects without any installed JVM/JRE.

Windows requires some setup for building graal native images.  [Chocolatey](https://chocolatey.org/install) package
 manager enables some easy configuration. Follow these steps to configure your Windows environment for building graal
  native images (tested on Windows 10 Pro):

```
   choco install visualstudio2019-workload-vctools windows-sdk-7.1 kb2519277
```

If an appropriate JDK is not installed, you may install a compatible JDK using chocolatey as well (note this may change
 your system PATH if you have a different JDK already installed):

```
    choco install adoptopenjdk11
```

If you encounter errors relating 'missing Windows 7.1 SDK', you can try specifying the visual studio version (2019 if following the install instructions just above), or provide the path to the appropriate `vsvars64.bat` by using the graal plugin configuration as [documented at the plugin repo](https://github.com/palantir/gradle-graal).

See the official [Graal Native-Image docs](https://www.graalvm.org/docs/reference-manual/native-image/) for details on
environmental prerequisites.


## Questions?  Feedback?  Want to Contribute?

Open an Issue.
