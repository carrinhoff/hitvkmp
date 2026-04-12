plugins {
    id("hitv.kmp.library")
    id("hitv.koin")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:model"))
            implementation(project(":shared:core:common"))
            implementation(project(":shared:core:domain"))
            implementation(project(":shared:core:database"))
            implementation(project(":shared:core:network"))
            implementation(project(":shared:epg"))
            implementation(project(":shared:core:billing"))
            implementation(libs.findLibrary("paging-common").get())
            implementation(libs.findLibrary("firebase-database").get())
            implementation(libs.findLibrary("kotlinx-datetime").get())
        }
        androidMain.dependencies {
            implementation(libs.findLibrary("androidx-security-crypto").get())
        }
    }
}
