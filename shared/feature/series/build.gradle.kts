plugins {
    id("hitv.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:data"))
            implementation(project(":shared:core:sync"))
            implementation(project(":shared:core:ui"))
            implementation(project(":shared:feature:player"))
            // Pulls in ParentalSessionGuard for gating the series tab.
            implementation(project(":shared:feature:settings"))
            implementation(libs.findLibrary("paging-compose").get())
            implementation(libs.findLibrary("coil-compose").get())
        }
        androidMain.dependencies {
            implementation(libs.findLibrary("androidx-tv-foundation").get())
            implementation(libs.findLibrary("androidx-tv-material").get())
        }
    }
}
