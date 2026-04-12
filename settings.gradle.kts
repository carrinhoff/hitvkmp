pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "HITV-KMP"

// Android App
include(":androidApp")

// Shared Core Modules
include(":shared:core:model")
include(":shared:core:common")
include(":shared:core:domain")
include(":shared:core:data")
include(":shared:core:database")
include(":shared:core:network")
include(":shared:core:designsystem")
include(":shared:core:navigation")
include(":shared:core:ui")
include(":shared:core:sync")
include(":shared:core:billing")
include(":shared:core:testing")

// Shared Feature Modules
include(":shared:feature:auth")
include(":shared:feature:channels")
include(":shared:feature:movies")
include(":shared:feature:series")
include(":shared:feature:player")
include(":shared:feature:settings")
include(":shared:feature:premium")

// EPG Module
include(":shared:epg")

// iOS umbrella framework module
include(":shared:umbrella")
