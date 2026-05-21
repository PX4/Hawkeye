import com.android.build.api.dsl.ApplicationExtension
import com.px4.hawkeye.buildlogic.PROJECT_TARGET_SDK
import com.px4.hawkeye.buildlogic.configureAndroidCommon
import com.px4.hawkeye.buildlogic.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                configureAndroidCommon(this)
                configureAndroidCompose(this)
                defaultConfig.targetSdk = PROJECT_TARGET_SDK
            }
        }
    }
}
