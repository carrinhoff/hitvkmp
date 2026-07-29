plugins {
    id("hitv.kmp.library")
    id("hitv.sqldelight")
    id("hitv.koin")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:core:model"))
            implementation(libs.findLibrary("paging-common").get())
            implementation(libs.findLibrary("kotlinx-datetime").get())
            implementation(libs.findLibrary("kotlinx-serialization-json").get())
        }
        // The JDBC sqlite-driver used by the androidUnitTest suite (in-memory SQLite, no emulator
        // needed) is supplied by the hitv.sqldelight convention plugin.
        //
        // androidInstrumentedTest runs the same kind of checks on a device/emulator against
        // Android's own SQLite through AndroidSqliteDriver — the closest available analogue to the
        // NativeSqliteDriver iOS uses, and the only local way to prove the schema and queries work
        // on an embedded SQLite rather than the desktop build the JVM tests link.
        androidInstrumentedTest.dependencies {
            implementation(libs.findLibrary("androidx-test-runner").get())
            implementation(libs.findLibrary("androidx-test-ext-junit").get())
            implementation(libs.findLibrary("kotlinx-coroutines-test").get())
            implementation(kotlin("test"))
        }
    }
}
