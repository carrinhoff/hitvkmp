import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("hitv.kmp.library")
            pluginManager.apply("hitv.compose.multiplatform")
            pluginManager.apply("hitv.koin")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies {
                    implementation(project(":shared:core:model"))
                    implementation(project(":shared:core:common"))
                    implementation(project(":shared:core:domain"))
                    implementation(project(":shared:core:designsystem"))
                    implementation(project(":shared:core:navigation"))
                    implementation(project.libs.findLibrary("voyager-navigator").get())
                    implementation(project.libs.findLibrary("voyager-screenModel").get())
                    implementation(project.libs.findLibrary("voyager-koin").get())
                    implementation(project.libs.findLibrary("voyager-transitions").get())
                }
            }
        }
    }
}
