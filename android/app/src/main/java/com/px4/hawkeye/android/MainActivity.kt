package com.px4.hawkeye.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.px4.hawkeye.android.shell.AppShell
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.feature.settings.domain.AppSettings
import com.px4.hawkeye.feature.settings.domain.SettingsRepository
import com.px4.hawkeye.feature.settings.domain.resolveDarkTheme
import org.koin.android.ext.android.inject

/** The launcher: a normal ComponentActivity hosting the Compose shell. */
class MainActivity : ComponentActivity() {

    private val settingsRepository: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            val darkTheme = settings.themeMode.resolveDarkTheme(isSystemInDarkTheme())
            HawkeyeTheme(darkTheme = darkTheme) { AppShell() }
        }
    }
}
