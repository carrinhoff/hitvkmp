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
        // KoinIOS.kt in iosMain imports these modules for Koin initialization
        iosMain.dependencies {
            implementation(project(":shared:core:data"))
            implementation(project(":shared:core:database"))
            implementation(project(":shared:core:network"))
            implementation(project(":shared:core:sync"))
            implementation(project(":shared:core:billing"))
            implementation(project(":shared:core:designsystem"))
        }
        androidMain.dependencies {
            implementation(libs.findLibrary("zxing-core").get())
            implementation(libs.findLibrary("androidx-security-crypto").get())
        }
    }
}
