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
            implementation(libs.findLibrary("kotlinx-datetime").get())
            // For asFlow()/mapToList() — reactive queries, so screens update when the DB changes.
            // core:database declares this too, but implementation-scoped so it doesn't propagate.
            implementation(libs.findLibrary("sqldelight-coroutines").get())
        }
        androidMain.dependencies {
            implementation(libs.findLibrary("androidx-security-crypto").get())
        }
        // JVM-only SQLDelight driver, so repository logic that is really database logic
        // (DifferentialChannelSync) can be tested against a real SQLite database instead of a
        // mock. Must stay on androidUnitTest, never commonTest — it has no iOS artifact and
        // putting it in commonTest breaks all iOS test compilation.
        androidUnitTest.dependencies {
            implementation(libs.findLibrary("sqldelight-sqlite-driver").get())
        }
        // Device tests: the repositories exercised on a real embedded SQLite through the real
        // AndroidSqliteDriver, which is the closest local analogue to the NativeSqliteDriver iOS
        // uses. This is where paging invalidation and the reactive flows are proven end to end.
        androidInstrumentedTest.dependencies {
            implementation(libs.findLibrary("androidx-test-runner").get())
            implementation(libs.findLibrary("androidx-test-ext-junit").get())
            implementation(libs.findLibrary("multiplatform-settings").get())
            implementation(kotlin("test"))
        }
    }
}
