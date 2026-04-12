plugins {
    id("hitv.kmp.library")
    id("hitv.compose.multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
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
    }
}
