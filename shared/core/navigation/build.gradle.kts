plugins {
    id("hitv.kmp.library")
    id("hitv.compose.multiplatform")
    alias(libs.plugins.kotlin.serialization)  // plugins block uses type-safe accessors
}

kotlin {
    // Override the framework baseName to "shared" so Swift imports it as "import shared"
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Framework>().configureEach {
            baseName = "shared"
            export(project(":shared:core:model"))
            export(project(":shared:core:common"))
            export(project(":shared:core:designsystem"))
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:core:model"))
            api(project(":shared:core:common"))
            api(project(":shared:core:designsystem"))
            implementation(project(":shared:core:sync"))
            implementation(project(":shared:core:ui"))
            implementation(libs.findLibrary("voyager-navigator").get())
            implementation(libs.findLibrary("voyager-transitions").get())
            implementation(libs.findLibrary("voyager-tabNavigator").get())
            implementation(libs.findLibrary("kotlinx-serialization-json").get())
            implementation(libs.findLibrary("koin-compose").get())
        }
        // KoinIOS.kt needs these for Koin module aggregation
        iosMain.dependencies {
            implementation(project(":shared:core:data"))
            implementation(project(":shared:core:database"))
            implementation(project(":shared:core:network"))
            implementation(project(":shared:core:billing"))
        }
    }
}
