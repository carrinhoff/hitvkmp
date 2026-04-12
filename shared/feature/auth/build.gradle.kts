plugins {
    id("hitv.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:data"))
            implementation(libs.findLibrary("firebase-database").get())
            implementation(libs.findLibrary("kotlinx-datetime").get())
        }
    }
}
