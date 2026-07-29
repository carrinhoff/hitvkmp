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

                    // ---------------------------------------------------------------------
                    // Schema snapshots + migration verification: OFF, deliberately, and this is a
                    // known gap rather than a decision that it is unneeded. See §4.1 / §5.
                    //
                    // The original carries 25 Room migrations (`AppDatabase` is version 25 and
                    // DatabaseModule chains MIGRATION_1_2 .. MIGRATION_24_25). The port has none,
                    // and no way to notice: with zero `.sqm` files SQLDelight pins the schema at
                    // version 1 permanently, so editing a `.sq` changes what a fresh install
                    // creates while leaving every existing install untouched. Nothing fails at
                    // build time; the app then queries a column its own database does not have.
                    // A first release is unaffected, which is exactly why this reaches users on
                    // the second one.
                    //
                    // The fix is these two lines:
                    //
                    //     schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
                    //     verifyMigrations.set(true)
                    //
                    // followed by `./gradlew generateCommonMainHitvDatabaseSchema` to commit the
                    // `databases/1.db` baseline. They are not enabled here because they cannot be
                    // made to work on this Windows host: SQLDelight 2.0.2's Gradle plugin resolves
                    // sqlite-jdbc 3.34.0, whose native library fails to load with
                    // "'void org.sqlite.core.NativeDB._open_utf8(byte[], int)'". Forcing a newer
                    // sqlite-jdbc onto the root buildscript classpath does not reach the plugin's
                    // worker. `verifyMigrations` attaches to `check`, so switching it on without
                    // that resolved breaks `./gradlew check` and `build` locally — trading a real
                    // regression for a guard that could not be verified here.
                    //
                    // Enable when building on Linux/macOS, or once the plugin is on a sqlite-jdbc
                    // whose native library loads on Windows.
                    // ---------------------------------------------------------------------
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
                // JDBC driver, JVM-only — belongs on the Android unit-test source set, NOT
                // commonTest. On commonTest it is also pulled into
                // `iosSimulatorArm64TestCompileKlibraries`, where `sqlite-driver` has no Kotlin/Native
                // artifact, so every SqlDelight module fails to compile its iOS tests with
                // "Could not resolve app.cash.sqldelight:sqlite-driver". That stayed invisible for as
                // long as nothing compiled iOS test code; it breaks the moment `iosSimulatorArm64Test`
                // runs. iOS tests that need a driver should use the native driver from iosMain.
                sourceSets.androidUnitTest.dependencies {
                    implementation(project.libs.findLibrary("sqldelight-sqlite-driver").get())
                }
            }
        }
    }
}
