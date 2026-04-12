import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<ApplicationExtension> {
                namespace = "pt.hitv.android"
                compileSdk = project.libs.findVersion("androidCompileSdk").get().toString().toInt()
                defaultConfig {
                    applicationId = "pt.television.hitv.kmp"
                    minSdk = project.libs.findVersion("androidMinSdk").get().toString().toInt()
                    targetSdk = project.libs.findVersion("androidTargetSdk").get().toString().toInt()
                    versionCode = 1
                    versionName = "1.0.0"
                }
                buildFeatures {
                    compose = true
                    buildConfig = true
                }
                compileOptions {
                    isCoreLibraryDesugaringEnabled = true
                    sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
                    targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
                }
            }

            // Align Kotlin JVM target with Java
            extensions.configure<KotlinAndroidProjectExtension> {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }

            // Add core library desugaring dependency
            dependencies.add("coreLibraryDesugaring", "com.android.tools:desugar_jdk_libs:2.1.4")
        }
    }
}
