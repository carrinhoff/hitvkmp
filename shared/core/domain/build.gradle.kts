plugins {
    id("hitv.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:model"))
            implementation(project(":shared:core:common"))
            implementation(project(":shared:epg"))
            implementation(libs.findLibrary("paging-common").get())
            implementation(libs.findLibrary("koin-core").get())
        }
    }
}
