# Ignition Module Generator

Consists of two projects useful in generating the boilerplate structure for an Ignition module built using the
Gradle build tool.

1. [Ignition Module Generator Core](https://github.com/inductiveautomation/ignition-module-tools/tree/master/generator/generator-core#readme) library for creating Ignition module skeletons.  Written in Kotlin, usable in Kotlin, Java and other JVM languages via jar-based dependency available as Maven artifacts.

2. [Module Generator CLI](https://github.com/inductiveautomation/ignition-module-tools/tree/master/generator/generator-cli#readme) command line application which uses the generator library to provide a _command line interface_ (CLI) that can be used to create modules from your operating system's terminal/console.

## Usage

#### Building the projects

To build the projects, cd into `<root>/generator` and run the `build` task via the included gradle wrapper:

```shell
// posix-style systems like Mac/Linux
./gradlew build
```

```
// windows
gradlew.bat build
```

### Running the CLI

To run the CLI locally, cd into the `generator/` directory and run `./gradlew runCli --console plain` task, (windows
users, use the _bat_ wrappers as shown above.  `--console plain ` is optional but suggested to avoid interpolation of
gradle console logging while the commandline prompts for user input.
