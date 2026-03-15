plugins {
    id("hitv.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:data"))
            implementation(project(":shared:core:ui"))
            implementation(project(":shared:epg"))
            implementation(libs.paging.compose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.tv.foundation)
            implementation(libs.androidx.tv.material)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
        }
    }
}
