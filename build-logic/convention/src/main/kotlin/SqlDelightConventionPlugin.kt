import app.cash.sqldelight.gradle.SqlDelightExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class SqlDelightConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("app.cash.sqldelight")

            extensions.configure<SqlDelightExtension> {
                databases.create("HitvDatabase") {
                    packageName.set("pt.hitv.core.database")
                    // Use SQLite 3.38 dialect for REPLACE() function support
                    dialect(project.libs.findLibrary("sqldelight-dialect").get())
                }
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies {
                    implementation(project.libs.findLibrary("sqldelight-runtime").get())
                    implementation(project.libs.findLibrary("sqldelight-coroutines").get())
                }
                sourceSets.androidMain.dependencies {
                    implementation(project.libs.findLibrary("sqldelight-android-driver").get())
                }
                sourceSets.iosMain.dependencies {
                    implementation(project.libs.findLibrary("sqldelight-native-driver").get())
                }
                sourceSets.commonTest.dependencies {
                    implementation(project.libs.findLibrary("sqldelight-sqlite-driver").get())
                }
            }
        }
    }
}
