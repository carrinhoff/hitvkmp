plugins {
    id("hitv.android.application")
}

dependencies {
    // All shared modules
    implementation(project(":shared:core:model"))
    implementation(project(":shared:core:common"))
    implementation(project(":shared:core:domain"))
    implementation(project(":shared:core:data"))
    implementation(project(":shared:core:database"))
    implementation(project(":shared:core:network"))
    implementation(project(":shared:core:designsystem"))
    implementation(project(":shared:core:navigation"))
    implementation(project(":shared:core:ui"))
    implementation(project(":shared:core:sync"))
    implementation(project(":shared:core:billing"))
    implementation(project(":shared:feature:auth"))
    implementation(project(":shared:feature:channels"))
    implementation(project(":shared:feature:movies"))
    implementation(project(":shared:feature:series"))
    implementation(project(":shared:feature:player"))
    implementation(project(":shared:feature:settings"))
    implementation(project(":shared:feature:premium"))
    implementation(project(":shared:epg"))

    // Multiplatform Settings (needed for AndroidPlatformModule)
    implementation(libs.findLibrary("multiplatform-settings").get())

    // Android-specific
    // Note: type-safe accessors unavailable when convention plugin is from included build.
    // Using libs.findLibrary() as workaround.
    implementation(libs.findLibrary("androidx-core-ktx").get())
    implementation(libs.findLibrary("androidx-appcompat").get())
    implementation(libs.findLibrary("androidx-activity-compose").get())
    implementation(libs.findLibrary("androidx-core-splashscreen").get())
    implementation(libs.findLibrary("androidx-lifecycle-process").get())

    // Koin Android
    implementation(libs.findLibrary("koin-android").get())
    implementation(libs.findLibrary("koin-compose").get())
    implementation(libs.findLibrary("koin-compose-viewmodel").get())

    // Firebase KMP
    implementation(libs.findLibrary("firebase-analytics").get())
    implementation(libs.findLibrary("firebase-crashlytics").get())
    implementation(libs.findLibrary("firebase-config").get())
    implementation(libs.findLibrary("firebase-database").get())

    // Media3
    implementation(libs.findLibrary("androidx-media3-exoplayer").get())
    implementation(libs.findLibrary("androidx-media3-exoplayer-hls").get())
    implementation(libs.findLibrary("androidx-media3-exoplayer-dash").get())
    implementation(libs.findLibrary("androidx-media3-exoplayer-smoothstreaming").get())
    implementation(libs.findLibrary("androidx-media3-cast").get())
    implementation(libs.findLibrary("androidx-media3-ui").get())

    // Cast
    implementation(libs.findLibrary("play-services-cast").get())
    implementation(libs.findLibrary("play-services-cast-framework").get())
    implementation(libs.findLibrary("androidx-mediarouter").get())

    // WorkManager
    implementation(libs.findLibrary("androidx-work-runtime-ktx").get())

    // TV
    implementation(libs.findLibrary("androidx-tv-foundation").get())
    implementation(libs.findLibrary("androidx-tv-material").get())
}
