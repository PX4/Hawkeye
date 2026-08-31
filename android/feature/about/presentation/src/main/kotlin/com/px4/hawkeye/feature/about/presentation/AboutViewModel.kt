package com.px4.hawkeye.feature.about.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.px4.hawkeye.core.presentation.AboutInfoProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AboutViewModel(
    private val aboutInfo: AboutInfoProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(AboutState(versionName = aboutInfo.versionName))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // A missing or unreadable notices asset must not take down the whole screen:
            // the version and disclaimer text still matter, so fall back to null and let
            // the screen point at the repository instead.
            val notices = runCatching { aboutInfo.loadNotices() }.getOrNull()
            _state.update { it.copy(notices = notices) }
        }
    }

    fun onAction(action: AboutAction) {
        when (action) {
            AboutAction.OnToggleLicenses ->
                _state.update { it.copy(isLicensesExpanded = !it.isLicensesExpanded) }
        }
    }
}
