plugins {
    id("hitv.kmp.library")
    id("hitv.koin")
}

kotlin {
    // Override framework name to "shared" for the iOS umbrella
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Framework>().configureEach {
            baseName = "shared"
            export(project(":shared:core:model"))
            export(project(":shared:core:common"))
            export(project(":shared:core:navigation"))
            export(project(":shared:core:designsystem"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Re-export core modules
            api(project(":shared:core:model"))
            api(project(":shared:core:common"))
            api(project(":shared:core:navigation"))
            api(project(":shared:core:designsystem"))
            api(project(":shared:core:data"))
            api(project(":shared:core:database"))
            api(project(":shared:core:network"))
            api(project(":shared:core:sync"))
            api(project(":shared:core:billing"))
            api(project(":shared:core:ui"))
            // Feature modules
            api(project(":shared:feature:auth"))
            api(project(":shared:feature:channels"))
            api(project(":shared:feature:movies"))
            api(project(":shared:feature:series"))
            api(project(":shared:feature:player"))
            api(project(":shared:feature:premium"))
            api(project(":shared:feature:settings"))
            // EPG
            api(project(":shared:epg"))
        }
        // SyncBridge.kt in iosMain references org.koin.core.context.GlobalContext.
        // The koin-core dep is implementation-scoped on upstream modules so doesn't
        // propagate transitively, AND `id("hitv.koin")` + `iosMain.dependencies {}`
        // both proved insufficient across CI runs. Declaring on each per-target
        // main source set directly, plus on commonMain as a belt-and-braces, since
        // SOMETHING about the source-set hierarchy on this module isn't routing
        // dependencies the way it should.
        commonMain.dependencies {
            implementation(libs.findLibrary("koin-core").get())
        }
        iosMain.dependencies {
            implementation(libs.findLibrary("koin-core").get())
        }
        getByName("iosArm64Main").dependencies {
            implementation(libs.findLibrary("koin-core").get())
        }
        getByName("iosX64Main").dependencies {
            implementation(libs.findLibrary("koin-core").get())
        }
        getByName("iosSimulatorArm64Main").dependencies {
            implementation(libs.findLibrary("koin-core").get())
        }
    }
}
