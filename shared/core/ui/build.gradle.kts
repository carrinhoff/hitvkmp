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
            implementation(libs.coil.compose)
            implementation(libs.paging.compose)
            implementation(libs.compottie)
        }
        androidMain.dependencies {
            implementation(libs.androidx.tv.foundation)
            implementation(libs.androidx.tv.material)
        }
    }
}
