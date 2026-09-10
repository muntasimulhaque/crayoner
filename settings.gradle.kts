pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "crayoner"
// The rules live in their own module: :app and :tools both depend on it, so
// the coloring engine and the picture data have exactly one home and the
// compiler enforces that they never import Android.
include(":core")
include(":app")
include(":tools")
