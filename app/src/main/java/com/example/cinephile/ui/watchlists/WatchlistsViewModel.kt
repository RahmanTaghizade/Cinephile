package com.example.cinephile.ui.watchlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.WatchlistRepository
import com.example.cinephile.domain.repository.WatchlistUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchlistsViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    val watchlists: StateFlow<List<WatchlistUiModel>> =
        flow {
            emitAll(watchlistRepository.getAllWatchlists())
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val currentId: StateFlow<Long?> =
        flow {
            emitAll(watchlistRepository.getCurrentWatchlist())
        }.map { it?.id }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun createNewWatchlist() {
        viewModelScope.launch {
            val existing = watchlists.value
            val nextIndex = nextDefaultIndex(existing.map { it.name })
            val name = "Watchlist $nextIndex"
            watchlistRepository.createWatchlist(name)
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
            if (newName.isBlank()) return@launch
            watchlistRepository.renameWatchlist(watchlistId, newName.trim())
        }
    }

    fun delete(watchlistId: Long) {
        viewModelScope.launch {
            watchlistRepository.deleteWatchlist(watchlistId)
        }
    }

    companion object {
        private val DEFAULT_NAME_REGEX = Regex("^Watchlist\\s+(\\d+)$")
    }
}


