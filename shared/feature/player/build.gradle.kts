plugins {
    id("hitv.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:data"))
        }
        commonTest.dependencies {
            // Virtual-time control for PlaybackStartWatchdog's delay-based deadline.
            implementation(libs.findLibrary("kotlinx-coroutines-test").get())
        }
        androidMain.dependencies {
            implementation(libs.findLibrary("androidx-media3-exoplayer").get())
            implementation(libs.findLibrary("androidx-media3-exoplayer-hls").get())
            implementation(libs.findLibrary("androidx-media3-exoplayer-dash").get())
            implementation(libs.findLibrary("androidx-media3-exoplayer-smoothstreaming").get())
            implementation(libs.findLibrary("androidx-media3-cast").get())
            implementation(libs.findLibrary("androidx-media3-ui").get())
        }
    }
}
