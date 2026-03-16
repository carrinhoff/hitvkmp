# ===== Ktor =====
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ===== kotlinx.serialization =====
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class pt.hitv.**$$serializer { *; }
-keepclassmembers class pt.hitv.** { *** Companion; }
-keepclasseswithmembers class pt.hitv.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ===== SQLDelight =====
-keep class app.cash.sqldelight.** { *; }

# ===== Koin =====
-keep class org.koin.** { *; }

# ===== Firebase (GitLive KMP) =====
-keep class com.google.firebase.** { *; }
-keep class dev.gitlive.firebase.** { *; }
-dontwarn dev.gitlive.firebase.**

# ===== Voyager Navigation =====
-keep class cafe.adriel.voyager.** { *; }

# ===== Compose =====
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ===== Media3 / ExoPlayer =====
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ===== Google Cast =====
-keep class com.google.android.gms.cast.** { *; }
-dontwarn com.google.android.gms.cast.**

# ===== General =====
# Keep all model classes for serialization
-keep class pt.hitv.core.model.** { *; }
-keep class pt.hitv.core.network.model.** { *; }
