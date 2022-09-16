plugins {
    base
}

val test by tasks.registering
val intTest by tasks.registering

/**
 * Bind top-level tasks to sub-build tasks for convenience and improved IDE behavior
 */
tasks {
    build {
        dependsOn(pluginTask("build"))
        dependsOn(generatorCoreTask("build"))
        dependsOn(generatorCliTask("build"))
    }
    assemble {
        dependsOn(pluginTask("assemble"))
        dependsOn(generatorCoreTask("assemble"))
        dependsOn(generatorCliTask("assemble"))
    }
    test {
        dependsOn(pluginTask("test"))
        dependsOn(generatorCoreTask("test"))
        dependsOn(generatorCliTask("test"))
    }
    intTest {
        dependsOn(pluginTask("integrationTest"))
        dependsOn(generatorCoreTask("integrationTest"))
        dependsOn(generatorCliTask("integrationTest"))
    }
    check {
        dependsOn(pluginTask("check"))
        dependsOn(generatorCoreTask("check"))
        dependsOn(generatorCliTask("check"))
    }
    clean {
        dependsOn(pluginTask("clean"))
        dependsOn(generatorCoreTask("clean"))
        dependsOn(generatorCliTask("clean"))
    }
    tasks {
        dependsOn(pluginTask("tasks"))
        dependsOn(generatorCoreTask("tasks"))
        dependsOn(generatorCliTask("tasks"))
    }
    addRule("Pattern: plugin<Taskname>") {
        val taskName: String = this
        if (taskName.startsWith("plugin")) {
            create(taskName) {
                dependsOn(pluginTask(taskName.replace("plugin", "").decapitalize()))
            }
        }
    }
    addRule("Pattern: gen<Taskname>") {
        val taskName:String = this
        if (taskName.startsWith("gen")) {
            create(taskName) {
                dependsOn(generatorCoreTask(taskName.replace("gen", "").decapitalize()))
            }
        }
    }
    addRule("Pattern: cli<Taskname>") {
        val taskName:String = this
        if (taskName.startsWith("cli")) {
            create(taskName) {
                dependsOn(generatorCliTask( taskName.replace("cli", "").decapitalize()))
            }
        }
    }
}

/* returns a reference to a task from the app(lication) build */
fun pluginTask(task: String): TaskReference {
    return gradle.includedBuild("gradle-module-plugin").task(":$task")
}
fun generatorCoreTask(task: String): TaskReference {
    return gradle.includedBuild("generator").task(":generator-core:$task")
}
fun generatorCliTask(task: String): TaskReference {
    return gradle.includedBuild("generator").task(":generator-cli:$task")
}
