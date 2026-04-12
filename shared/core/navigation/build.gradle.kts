plugins {
    id("hitv.kmp.library")
    id("hitv.compose.multiplatform")
    alias(libs.plugins.kotlin.serialization)  // plugins block uses type-safe accessors
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:model"))
            implementation(project(":shared:core:common"))
            implementation(project(":shared:core:designsystem"))
            implementation(project(":shared:core:sync"))
            implementation(project(":shared:core:ui"))
            implementation(libs.findLibrary("voyager-navigator").get())
            implementation(libs.findLibrary("voyager-transitions").get())
            implementation(libs.findLibrary("voyager-tabNavigator").get())
            implementation(libs.findLibrary("kotlinx-serialization-json").get())
            implementation(libs.findLibrary("koin-compose").get())
        }
    }
}
