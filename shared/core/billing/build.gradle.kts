plugins {
    id("hitv.kmp.library")
    id("hitv.koin")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:common"))
        }
        androidMain.dependencies {
            implementation(libs.billing)
            implementation(libs.billing.ktx)
        }
    }
}
