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

rootProject.name = "SuperApp"

include(":app")

include(":core:common")
include(":core:designsystem")
include(":core:ui")
include(":core:network")
include(":core:database")
include(":core:data")
include(":core:domain")

include(":feature:auth")
include(":feature:featureA")
include(":feature:featureB")
include(":feature:featureC")
