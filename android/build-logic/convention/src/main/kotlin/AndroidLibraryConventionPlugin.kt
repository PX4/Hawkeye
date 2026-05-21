import com.android.build.api.dsl.LibraryExtension
import com.px4.hawkeye.buildlogic.configureAndroidCommon
import com.px4.hawkeye.buildlogic.lib
import com.px4.hawkeye.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                configureAndroidCommon(this)
                testOptions.unitTests.isReturnDefaultValues = true
            }

            dependencies {
                add("testImplementation", libs.lib("junit-jupiter"))
                add("testRuntimeOnly", libs.lib("junit-jupiter-engine"))
                add("testRuntimeOnly", libs.lib("junit-platform-launcher"))
                add("testImplementation", libs.lib("assertk"))
                add("testImplementation", libs.lib("turbine"))
                add("testImplementation", libs.lib("kotlinx-coroutines-test"))
            }

            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
            }
        }
    }
}
