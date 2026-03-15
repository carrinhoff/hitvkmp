plugins {
    id("hitv.kmp.library")
    id("hitv.koin")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:common"))
            implementation(project(":shared:core:data"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.work.runtime.ktx)
        }
    }
}
