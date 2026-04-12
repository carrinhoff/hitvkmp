plugins {
    id("hitv.kmp.library")
    id("hitv.compose.multiplatform")
    id("hitv.koin")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:model"))
            implementation(project(":shared:core:common"))
            implementation(project(":shared:core:domain"))
            implementation(project(":shared:core:designsystem"))
            implementation(libs.findLibrary("coil-compose").get())
            implementation(libs.findLibrary("kotlinx-datetime").get())
            implementation(libs.findLibrary("paging-compose").get())
            implementation(libs.findLibrary("compottie").get())
        }
        androidMain.dependencies {
            implementation(libs.findLibrary("androidx-tv-foundation").get())
            implementation(libs.findLibrary("androidx-tv-material").get())
        }
    }
}
