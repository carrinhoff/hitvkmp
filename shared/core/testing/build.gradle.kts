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
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.datetime)
            implementation(libs.paging.common)
            implementation(kotlin("test"))
        }
    }
}
