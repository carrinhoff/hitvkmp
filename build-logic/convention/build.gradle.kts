plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.sqldelight.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "hitv.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpFeature") {
            id = "hitv.kmp.feature"
            implementationClass = "KmpFeatureConventionPlugin"
        }
        register("androidApp") {
            id = "hitv.android.application"
            implementationClass = "AndroidAppConventionPlugin"
        }
        register("composeMultiplatform") {
            id = "hitv.compose.multiplatform"
            implementationClass = "ComposeMultiplatformConventionPlugin"
        }
        register("koinConvention") {
            id = "hitv.koin"
            implementationClass = "KoinConventionPlugin"
        }
        register("sqldelightConvention") {
            id = "hitv.sqldelight"
            implementationClass = "SqlDelightConventionPlugin"
        }
    }
}
