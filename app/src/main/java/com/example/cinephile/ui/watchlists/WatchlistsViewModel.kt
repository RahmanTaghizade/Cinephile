package com.example.cinephile.ui.watchlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.WatchlistRepository
import com.example.cinephile.domain.repository.WatchlistUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchlistsViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _watchlistsFlow = MutableStateFlow<kotlinx.coroutines.flow.Flow<List<WatchlistUiModel>>?>(null)
    
    val watchlists: StateFlow<List<WatchlistUiModel>> = _watchlistsFlow
        .flatMapLatest { it ?: flowOf(emptyList()) }
        .catch { e ->
            _error.value = e.message ?: "Error loading watchlists"
            _isLoading.value = false
            emit(emptyList())
        }
        .map { list ->
            _isLoading.value = false
            _error.value = null
            list
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentWatchlistFlow = MutableStateFlow<kotlinx.coroutines.flow.Flow<WatchlistUiModel?>?>(null)
    
    val currentId: StateFlow<Long?> = _currentWatchlistFlow
        .flatMapLatest { it ?: flowOf(null) }
        .map { it?.id }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val watchlistsFlow = watchlistRepository.getAllWatchlists()
                _watchlistsFlow.value = watchlistsFlow
                
                val currentFlow = watchlistRepository.getCurrentWatchlist()
                _currentWatchlistFlow.value = currentFlow
            } catch (e: Exception) {
                _error.value = e.message ?: "Error loading watchlists"
                _isLoading.value = false
            }
        }
    }
    
    fun retry() {
        viewModelScope.launch {
            _error.value = null
            _isLoading.value = true
            try {
                val flow = watchlistRepository.getAllWatchlists()
                _watchlistsFlow.value = flow
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = e.message ?: "Error loading watchlists"
            }
        }
    }

    fun createNewWatchlist() {
        viewModelScope.launch {
            try {
                val existing = watchlists.value
                val nextIndex = nextDefaultIndex(existing.map { it.name })
                val name = "Watchlist $nextIndex"
                watchlistRepository.createWatchlist(name)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error creating watchlist"
            }
        }
    }

    private fun nextDefaultIndex(existingNames: List<String>): Int {
        var max = 0
        for (n in existingNames) {
            val match = DEFAULT_NAME_REGEX.matchEntire(n)
            if (match != null) {
                val num = match.groupValues[1].toIntOrNull() ?: continue
                if (num > max) max = num
            }
        }
        return max + 1
    }

    fun rename(watchlistId: Long, newName: String) {
        viewModelScope.launch {
            try {
                if (newName.isBlank()) return@launch
                watchlistRepository.renameWatchlist(watchlistId, newName.trim())
            } catch (e: Exception) {
                _error.value = e.message ?: "Error renaming watchlist"
            }
        }
    }

    fun delete(watchlistId: Long) {
        viewModelScope.launch {
            try {
                watchlistRepository.deleteWatchlist(watchlistId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error deleting watchlist"
            }
        }
    }

    companion object {
        private val DEFAULT_NAME_REGEX = Regex("^Watchlist\\s+(\\d+)$")
    }
}


