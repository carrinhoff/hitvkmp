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
            implementation(libs.paging.common)
            implementation(libs.firebase.database)
            implementation(libs.kotlinx.datetime)
        }
    }
}
