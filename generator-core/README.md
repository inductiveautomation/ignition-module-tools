# Ignition Module Generator

A library for creating folder/file structures for gradle-based Ignition Module projects.

## Adding to Your Project

This small library is intended for use in a Java runtime environment, and is currently published to the Inductive Automation public repository.  To use in a project, add the IA artifact repo to your project's repositories, the add the dependency.

##### Inductive Automation Artifact Repo

```
// gradle
repositories {
    // gradle can resolve via the singlular public url
    maven { url 'https://nexus.inductiveautomation.com/repository/public'  }
}

// maven -- requires independent entries
<repositories>
        <repository>
            <id>releases</id>
            <url>https://nexus.inductiveautomation.com/repository/inductiveautomation-releases</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
            <releases>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </releases>
        </repository>

        <repository>
            <id>snapshots</id>
            <url>https://nexus.inductiveautomation.com/repository/inductiveautomation-snapshots</url>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
            <releases>
                <enabled>false</enabled>
            </releases>
        </repository>

        <repository>
            <id>thirdparty</id>
            <url>https://nexus.inductiveautomation.com/repository/inductiveautomation-thirdparty</url>
            <releases>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>

```



```
dependencies {
    implementation("io.ia.sdk.tools.module.gen:core:$VERSION")
}
```

#### Maven pom.xml
```
...
<dependency>
    <groupId>io.ia.sdk.tools.module.gen</groupId>
    <artifactId>core</artifactId>
    <version>$VERSION</version>
</dependency>

```

## Using 

To generate a module, simple create a GeneratorConfig by using the provided builder, and then call the `generate(config)` function.  


```java

import io.ia.ignition.module.generator.ModuleGenerator;
import io.ia.ignition.module.generator.api.GeneratorConfig.ConfigBuilder;

public class Demo {
    public static void main(String[] args) {
        Path parentDir = Paths.get(System.getProperty("user.home") + "/ignition/modules");

        ConfigBuilder builder = new ConfigBuilder();        
        builder.moduleName("Great Stuff");
        builder.scopes("G");
        builder.packageName("my.pkg.name");
        builder.parentDir(parentDir);
        
        Path moduleRoot = ModuleGenerator.generate(builder.build());

        System.out.println("New module generated at " + moduleRoot.toString());
    }
}
```


## Building

This project uses Gradle for build tooling and includes the gradle wrapper.  To build, simply execute `./gradlew build` on posix machines, or `gradle.bat build` on Windows.  The wrapper will download all necessary dependencies from the primary gradle.org server and then execute the build using the correct version of the grade build tool. 

To see a list of all tasks available, run `./gradlew tasks` or if on Windows, `gradle.bat tasks`.

## Publishing

#### Note: Publication support is planned, but not yet fully implemented

The assembled library may be published to an artifact repository by configuring the appropriate publishing settings.  
By default, it is configured to publish to a maven repository.  To publish using this default setup, set the following 
properties (with appropriate values) as environmental parameters.  This is most easily done with a 'gradle.properties' 
file, which can reside in the root of this repository, or in your user '.gradle' directory (
`~/.gradle/gradle.properties` in posix systems, typically `C:\Users\username\.gradle\gradle.properties` on Windows). 


```
ignitionModuleGen.maven.repo.snapshot.name=
ignitionModuleGen.maven.repo.snapshot.url=
ignitionModuleGen.maven.repo.snapshot.username=<
ignitionModuleGen.maven.repo.snapshot.password=
ignitionModuleGen.maven.repo.release.name=
ignitionModuleGen.maven.repo.release.url=
ignitionModuleGen.maven.repo.release.username=
ignitionModuleGen.maven.repo.release.password=
```


## Roadmap

### Planned

* [ ] Functional kotlin buildscript generation
* [ ] Fully functional kotlin-based modules
    * [ ] kotlin module sources
    

## Contributing

Contributions are welcome, open an Issue to discuss your ideas, or submit a PR for feedbpack!
