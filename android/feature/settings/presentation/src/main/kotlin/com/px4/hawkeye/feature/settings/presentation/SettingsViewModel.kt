package com.px4.hawkeye.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.px4.hawkeye.feature.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { s ->
                _state.update { it.copy(themeMode = s.themeMode, distanceUnit = s.distanceUnit) }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnThemeModeSelected ->
                viewModelScope.launch { repository.setThemeMode(action.mode) }
            is SettingsAction.OnDistanceUnitSelected ->
                viewModelScope.launch { repository.setDistanceUnit(action.unit) }
        }
    }
}
