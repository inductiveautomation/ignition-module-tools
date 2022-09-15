//<SETTINGS_HEADER>

rootProject.name = "<ROOT_PROJECT_NAME>"

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        maven {
            url = uri("https://nexus.inductiveautomation.com/repository/public")
        }
    }
}

include(
<SUBPROJECT_INCLUDES>
)
