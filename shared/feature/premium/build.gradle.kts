plugins {
    id("hitv.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:billing"))
            implementation(libs.findLibrary("kotlinx-datetime").get())
        }
    }
}
