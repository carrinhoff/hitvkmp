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
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.paging.compose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.tv.foundation)
            implementation(libs.androidx.tv.material)
        }
    }
}
