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

    // Android-specific
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.process)

    // Koin Android
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    // Firebase KMP
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.database)

    // Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.smoothstreaming)
    implementation(libs.androidx.media3.cast)
    implementation(libs.androidx.media3.ui)

    // Cast
    implementation(libs.play.services.cast)
    implementation(libs.play.services.cast.framework)
    implementation(libs.androidx.mediarouter)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // TV
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
}
