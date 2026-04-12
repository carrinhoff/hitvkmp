plugins {
    id("hitv.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:model"))
            implementation(project(":shared:core:common"))
            implementation(project(":shared:core:domain"))
            implementation(project(":shared:core:database"))
            implementation(libs.findLibrary("kotlinx-coroutines-test").get())
            implementation(libs.findLibrary("kotlinx-datetime").get())
            implementation(libs.findLibrary("paging-common").get())
            implementation(kotlin("test"))
        }
    }
}
