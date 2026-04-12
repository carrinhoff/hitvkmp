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
            implementation(libs.findLibrary("billing").get())
            implementation(libs.findLibrary("billing-ktx").get())
        }
    }
}
