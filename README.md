# Ignition Module Tools

This repository holds a number of tools that are used to create and/or support the creation of modules intended for Inductive Automation's Ignition platform.

## Contents

Contains three subprojects.  See the linked readmes for details

1. A plugin for the [Gradle](https://www.gradle.org) build system which provides the ability to assemble valid Ignition modules.

1. [Ignition Module Generator Core](https://github.com/inductiveautomation/ignition-module-tools/tree/master/generator/generator-core#readme) library for creating Ignition module skeletons.  Written in Kotlin, usable in Kotlin, Java and other JVM languages via jar-based dependency available as Maven artifacts.

2. [Module Generator CLI](https://github.com/inductiveautomation/ignition-module-tools/tree/master/generator/generator-cli#readme) command line application which uses the generator library to provide a _command line interface_ (CLI) that can be used to create modules from your operating system's terminal/console.


#### See the README.md in the appropriate subproject for more information about that project.

## Requirements

The only requirement is an appropriate JDK (JDK 11 as of writing) available on the system path.

# How to Build

This repo is structured as a composite [gradle](http://gradle.org) build. It includes the gradlew wrapper scripts for linux/mac/windows to enable dependency-free assembly.  All gradle commands should be executed using the wrapper appropriate for the platform being built on.  This generally means executing tasks via `gradlew.bat` for Windows, and the `gradlew` script on Linux/Mac.  Note that a `build` on Windows, Mac or Linux will output artifacts useful for all supported platforms, regardless of which platform the build is executed on.  However, the native image binaries of the CLI subproject are only created for the platform on which the build runs, and this must be executed on each platform for which a binary is desired.

To build all projects, from the root of the repository:

```
// windows
gradlew.bat build

// mac/linux
./gradlew build

```

The output of each assembly will exist in the local subproject's `build` directory, which is created during build
execution.  A specific/individual project can be built by executing `./gradlew build` in the appropriate project
subdirectory, or by using the task rules as described below.

By default, the following tasks are executable from the root directory using the gradle wrapper script:

* build
* clean
* assemble
* tasks
* test
* check
* intTest (maps to 'integrationTest' of the subprojects)


## Task Rules

For convenience, task rules have been created at the root level, to be able to run specific subproject tasks without
needing to change working directories in your terminal.  The rules are as follows (for all examples, replace `./gradlew`
with `gradlew.bat` for windows:

```shell
// run task named 'taskName' on the plugin subproject
./gradlew pluginTaskName

// run task named 'taskName' on the generator-core subproject
./gradlew genTaskName

// run task named 'taskName' on the generator-cli subproject
./gradlew cliTaskName
```

For example, to run the `spotlessApply` task on the generator-cli subproject, execute `./gradlew cliSpotlessApply`.

To see a list of all available tasks, run `./gradlew tasks` (`gradlew.bat tasks` on Windows).


## Questions?  Feedback?  Want to Contribute?

Open an Issue!
