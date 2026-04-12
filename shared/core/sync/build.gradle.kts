plugins {
    id("hitv.kmp.library")
    id("hitv.koin")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:common"))
            implementation(project(":shared:core:model"))
            implementation(project(":shared:core:domain"))
            implementation(project(":shared:core:data"))
            implementation(project(":shared:epg"))
        }
        androidMain.dependencies {
            implementation(libs.findLibrary("androidx-work-runtime-ktx").get())
        }
    }
}
