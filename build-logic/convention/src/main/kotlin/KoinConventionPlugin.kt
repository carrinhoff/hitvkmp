import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies {
                    implementation(project.libs.findLibrary("koin-core").get())
                    implementation(project.libs.findLibrary("koin-compose").get())
                    implementation(project.libs.findLibrary("koin-compose-viewmodel").get())
                }
                sourceSets.commonTest.dependencies {
                    implementation(project.libs.findLibrary("koin-test").get())
                }
            }
        }
    }
}
