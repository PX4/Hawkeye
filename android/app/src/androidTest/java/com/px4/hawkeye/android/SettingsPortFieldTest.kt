package com.px4.hawkeye.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import com.px4.hawkeye.core.presentation.UiText
import com.px4.hawkeye.feature.settings.presentation.SettingsScreen
import com.px4.hawkeye.feature.settings.presentation.SettingsState
import org.junit.Rule
import org.junit.Test

/** Verifies the Settings port field surfaces a validation error — no DataStore, no device state. */
class SettingsPortFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun portError_isShown_whenStateHasError() {
        composeRule.setContent {
            HawkeyeTheme {
                SettingsScreen(
                    state = SettingsState(
                        portInput = "80",
                        portError = UiText.DynamicString("Enter a port between 1024 and 65535."),
                    ),
                    onAction = {},
                    onNavigateToAbout = {},
                )
            }
        }
        composeRule.onNodeWithText("Enter a port between 1024 and 65535.").assertIsDisplayed()
    }

    @Test
    fun portError_isAbsent_whenStateHasNoError() {
        composeRule.setContent {
            HawkeyeTheme {
                SettingsScreen(
                    state = SettingsState(portInput = "19410", portError = null),
                    onAction = {},
                    onNavigateToAbout = {},
                )
            }
        }
        composeRule.onNodeWithText("Enter a port between 1024 and 65535.").assertDoesNotExist()
    }
}
