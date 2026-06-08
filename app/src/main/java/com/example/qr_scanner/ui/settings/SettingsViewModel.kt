package com.example.qr_scanner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.qr_scanner.data.history.ScanHistoryRepository
import com.example.qr_scanner.data.prefs.UserPreferencesRepository
import com.example.qr_scanner.ui.app
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferences: UserPreferencesRepository,
    private val history: ScanHistoryRepository,
) : ViewModel() {

    val saveHistoryEnabled: StateFlow<Boolean> =
        preferences.saveHistoryEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val excludeWifi: StateFlow<Boolean> =
        preferences.excludeWifiFromHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setSaveHistory(value: Boolean) {
        viewModelScope.launch { preferences.setSaveHistoryEnabled(value) }
    }

    fun setExcludeWifi(value: Boolean) {
        viewModelScope.launch { preferences.setExcludeWifi(value) }
    }

    fun clearHistory() {
        viewModelScope.launch { history.clearAll() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = app()
                SettingsViewModel(app.preferencesRepository, app.historyRepository)
            }
        }
    }
}
