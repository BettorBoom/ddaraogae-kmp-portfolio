pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Ddaraogae"
include(":domain", ":data", ":designsystem", ":feature:walk", ":feature:dogs", ":feature:history", ":sharedApp", ":androidApp")
