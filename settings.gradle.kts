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
        mavenCentral() // Required for Supabase SDK
    }
}

rootProject.name = "MomoTerminal"

// Main app module
include(":app")

// Core modules
include(":core:common")
include(":core:designsystem")
include(":core:ui")
include(":core:network")
include(":core:database")
include(":core:data")
include(":core:domain")
include(":core:os-integration")
include(":core:performance")
include(":core:i18n")
include(":core:security")
include(":core:ai")

// Feature modules
include(":feature:payment")
include(":feature:transactions")
include(":feature:auth")
include(":feature:settings")
include(":feature:nfc")
include(":feature:sms")
include(":feature:wallet")
include(":feature:vending")
include(":sms-bridge")

// Baseline profile
include(":baselineprofile")
