package com.example.cinephile.ui.watchlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.WatchlistRepository
import com.example.cinephile.domain.repository.WatchlistUiModel
import com.example.cinephile.ui.search.MovieUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchlistDetailsViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val watchlistId: Long = savedStateHandle.get<Long>("watchlistId") ?: 0L

    val watchlist: StateFlow<WatchlistUiModel?> =
        watchlistRepository.getWatchlistById(watchlistId)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val movies: StateFlow<List<MovieUiModel>> =
        flow {
            val source = watchlistRepository.getWatchlistMovies(watchlistId)
            emitAll(source)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun remove(movieId: Long) {
        viewModelScope.launch {
            watchlistRepository.removeFromWatchlist(watchlistId, movieId)
        }
    }

    fun add(movieId: Long) {
        viewModelScope.launch {
            watchlistRepository.addToWatchlist(watchlistId, movieId)
        }
    }

    fun rename(newName: String) {
        viewModelScope.launch {
            watchlistRepository.renameWatchlist(watchlistId, newName)
        }
    }

    fun delete() {
        viewModelScope.launch {
            watchlistRepository.deleteWatchlist(watchlistId)
        }
    }
}


