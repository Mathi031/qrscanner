package dev.mathi031.qrscanner.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.mathi031.qrscanner.data.ScanResult
import dev.mathi031.qrscanner.data.history.ScanHistoryRepository
import dev.mathi031.qrscanner.ui.app
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Carga un [ScanResult] histórico por id desde el repositorio. */
class ResultViewModel(
    private val historyRepository: ScanHistoryRepository,
) : ViewModel() {

    private val _result = MutableStateFlow<ScanResult?>(null)
    val result: StateFlow<ScanResult?> = _result.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            _result.value = historyRepository.getById(id)
            _loaded.value = true
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ResultViewModel(app().historyRepository) }
        }
    }
}
