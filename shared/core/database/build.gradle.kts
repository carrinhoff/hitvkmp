plugins {
    id("hitv.kmp.library")
    id("hitv.sqldelight")
    id("hitv.koin")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:model"))
            implementation(libs.findLibrary("paging-common").get())
            implementation(libs.findLibrary("kotlinx-datetime").get())
            implementation(libs.findLibrary("kotlinx-serialization-json").get())
        }
    }
}
