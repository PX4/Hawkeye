pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Hawkeye Android"
include(":app")
include(":core:domain")
include(":core:presentation")
include(":core:design-system")
include(":feature:replay:data")
include(":feature:replay:presentation")
include(":core:navigation")
include(":feature:settings:domain")
include(":feature:settings:data")
include(":feature:settings:presentation")
include(":feature:home:presentation")
