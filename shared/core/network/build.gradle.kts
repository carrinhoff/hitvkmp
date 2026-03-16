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
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.xz)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
