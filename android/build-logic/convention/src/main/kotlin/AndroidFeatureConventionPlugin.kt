import com.px4.hawkeye.buildlogic.libs
import com.px4.hawkeye.buildlogic.lib
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("hawkeye.android.library.compose")

            dependencies {
                add("implementation", libs.lib("androidx-core-ktx"))
                add("implementation", libs.lib("kotlinx-coroutines-android"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.lib("koin-android"))
                add("implementation", libs.lib("koin-androidx-compose"))
            }
        }
    }
}
