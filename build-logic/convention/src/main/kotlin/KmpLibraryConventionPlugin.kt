import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.library")

            extensions.configure<KotlinMultiplatformExtension> {
                androidTarget {
                    compilations.all {
                        kotlinOptions {
                            jvmTarget = "17"
                        }
                    }
                }
                listOf(
                    iosX64(),
                    iosArm64(),
                    iosSimulatorArm64()
                ).forEach {
                    it.binaries.framework {
                        baseName = project.path.replace(":", "-").drop(1)
                        isStatic = true
                    }
                }

                sourceSets.commonMain.dependencies {
                    implementation(project.libs.findLibrary("kotlinx-coroutines-core").get())
                }

                sourceSets.commonTest.dependencies {
                    implementation(kotlin("test"))
                    implementation(project.libs.findLibrary("kotlinx-coroutines-test").get())
                }
            }

            extensions.configure<com.android.build.gradle.LibraryExtension> {
                namespace = "pt.hitv.${project.path.replace(":", ".").drop(1).replace("shared.", "")}"
                compileSdk = project.libs.findVersion("androidCompileSdk").get().toString().toInt()
                defaultConfig {
                    minSdk = project.libs.findVersion("androidMinSdk").get().toString().toInt()
                    // Lets modules add `androidInstrumentedTest` sources. Those run on a real
                    // device/emulator against Android's own SQLite via AndroidSqliteDriver, rather
                    // than desktop SQLite via JdbcSqliteDriver as the JVM unit tests do — which is
                    // the only way to catch SQL that a bundled SQLite accepts and an embedded one
                    // does not.
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
                    targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
                }
                testOptions {
                    // androidUnitTest runs on a stub android.jar whose methods throw
                    // "Method ... not mocked" by default. Shared code that touches an Android API
                    // only incidentally — androidx Paging's invalidate() calls android.util.Log —
                    // then fails the test for a reason unrelated to what it asserts. Returning
                    // defaults keeps those tests exercising the real logic.
                    unitTests.isReturnDefaultValues = true
                }
            }
        }
    }
}
