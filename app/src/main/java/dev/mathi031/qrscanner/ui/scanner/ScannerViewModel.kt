package dev.mathi031.qrscanner.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.mathi031.qrscanner.data.BarcodeScannerAnalyzer
import dev.mathi031.qrscanner.data.ContentType
import dev.mathi031.qrscanner.data.ScanResult
import dev.mathi031.qrscanner.data.history.ScanHistoryRepository
import dev.mathi031.qrscanner.data.history.ScanSource
import dev.mathi031.qrscanner.data.prefs.UserPreferencesRepository
import dev.mathi031.qrscanner.ui.app
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannerUiState(
    val torchEnabled: Boolean = false,
    val hasFlashUnit: Boolean = false,
)

class ScannerViewModel(
    private val historyRepository: ScanHistoryRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    // El analyzer (cámara) reporta siempre source = CAMERA.
    val analyzer = BarcodeScannerAnalyzer(onResult = { onDetected(it, ScanSource.CAMERA) })

    private val _scanEvents = Channel<ScanResult>(Channel.BUFFERED)
    val scanEvents = _scanEvents.receiveAsFlow()

    fun toggleTorch() {
        _uiState.update { it.copy(torchEnabled = !it.torchEnabled) }
    }

    fun setHasFlashUnit(value: Boolean) {
        _uiState.update { it.copy(hasFlashUnit = value, torchEnabled = it.torchEnabled && value) }
    }

    /** Llamado por el analyzer (CAMERA) y por el escaneo de galería (GALLERY). */
    fun onDetected(result: ScanResult, source: ScanSource) {
        analyzer.pause()
        // Guardado independiente: no bloquea la navegación. El detalle de un
        // escaneo recién hecho se pasa por nav arg, no depende del id de la DB.
        viewModelScope.launch { maybeSave(result, source) }
        viewModelScope.launch { _scanEvents.send(result) }
    }

    /** Guarda en historial respetando los ajustes de privacidad. Fire-and-forget. */
    private suspend fun maybeSave(result: ScanResult, source: ScanSource) {
        if (!preferencesRepository.saveHistoryEnabled.first()) return
        if (result.detectedType == ContentType.WIFI &&
            preferencesRepository.excludeWifiFromHistory.first()
        ) {
            return
        }
        runCatching { historyRepository.save(result, source, System.currentTimeMillis()) }
    }

    fun resumeScanning() {
        analyzer.resume()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = app()
                ScannerViewModel(app.historyRepository, app.preferencesRepository)
            }
        }
    }
}
