plugins {
    id("hitv.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:data"))
            implementation(project(":shared:core:ui"))
            implementation(project(":shared:epg"))
            implementation(libs.findLibrary("paging-compose").get())
            implementation(libs.findLibrary("coil-compose").get())
        }
        androidMain.dependencies {
            implementation(libs.findLibrary("androidx-tv-foundation").get())
            implementation(libs.findLibrary("androidx-tv-material").get())
            implementation(libs.findLibrary("androidx-media3-exoplayer").get())
            implementation(libs.findLibrary("androidx-media3-ui").get())
        }
    }
}
