plugins {
    id("hitv.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:data"))
            implementation(project(":shared:core:ui"))
            implementation(project(":shared:core:billing"))
            implementation(project(":shared:core:sync"))
        }
        androidMain.dependencies {
            implementation(libs.findLibrary("androidx-tv-foundation").get())
            implementation(libs.findLibrary("androidx-tv-material").get())
        }
    }
}
