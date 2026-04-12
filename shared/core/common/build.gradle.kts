plugins {
    id("hitv.kmp.library")
    id("hitv.koin")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:model"))
            implementation(libs.findLibrary("multiplatform-settings").get())
            implementation(libs.findLibrary("multiplatform-settings-coroutines").get())
            implementation(libs.findLibrary("kotlinx-datetime").get())
        }
        androidMain.dependencies {
            implementation(libs.findLibrary("zxing-core").get())
            implementation(libs.findLibrary("androidx-security-crypto").get())
        }
    }
}
