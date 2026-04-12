plugins {
    id("hitv.kmp.library")
    alias(libs.plugins.kotlin.serialization)
    id("hitv.koin")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:model"))
            implementation(project(":shared:core:common"))
            implementation(libs.findLibrary("ktor-client-core").get())
            implementation(libs.findLibrary("ktor-client-content-negotiation").get())
            implementation(libs.findLibrary("ktor-client-logging").get())
            implementation(libs.findLibrary("ktor-serialization-kotlinx-json").get())
            implementation(libs.findLibrary("kotlinx-serialization-json").get())
        }
        androidMain.dependencies {
            implementation(libs.findLibrary("ktor-client-okhttp").get())
            implementation(libs.findLibrary("xz").get())
        }
        iosMain.dependencies {
            implementation(libs.findLibrary("ktor-client-darwin").get())
        }
    }
}
