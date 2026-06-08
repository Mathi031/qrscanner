package com.example.qr_scanner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.qr_scanner.data.history.HistoryItem
import com.example.qr_scanner.data.history.ScanHistoryRepository
import com.example.qr_scanner.ui.app
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HistoryViewModel(
    private val repository: ScanHistoryRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val items: StateFlow<List<HistoryItem>> = _query
        .debounce { if (it.isEmpty()) 0L else 200L } // debounce 200ms al escribir
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) repository.observeAll() else repository.search(q.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun toggleFavorite(item: HistoryItem) {
        viewModelScope.launch { repository.setFavorite(item.id, !item.isFavorite) }
    }

    fun delete(item: HistoryItem) {
        viewModelScope.launch { repository.delete(item.id) }
    }

    /** Re-inserta una entrada borrada (acción "Deshacer"). */
    fun restore(item: HistoryItem) {
        viewModelScope.launch { repository.restore(item) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HistoryViewModel(app().historyRepository) }
        }
    }
}
