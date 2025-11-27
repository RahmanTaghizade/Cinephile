package com.example.cinephile.ui.series

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.data.remote.TmdbService
import com.example.cinephile.data.remote.TmdbTvSeriesDetails
import com.example.cinephile.domain.repository.WatchlistRepository
import com.example.cinephile.domain.repository.WatchlistUiModel
import com.example.cinephile.ui.search.CastMember
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SeriesDetailsViewModel @Inject constructor(
    private val tmdbService: TmdbService,
    private val watchlistRepository: WatchlistRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesDetailsUiState())
    val uiState: StateFlow<SeriesDetailsUiState> = _uiState.asStateFlow()

    private val seriesId: Long = checkNotNull(savedStateHandle["seriesId"]) { "seriesId is required" }

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val details = withContext(Dispatchers.IO) {
                    kotlin.runCatching { tmdbService.getTvSeries(seriesId) }.getOrNull()
                }
                val credits = withContext(Dispatchers.IO) {
                    kotlin.runCatching { tmdbService.getTvCredits(seriesId) }.getOrNull()
                }

                val voteAverage = details?.voteAverage ?: 0.0
                val cast = credits?.cast?.sortedBy { it.order }?.take(10)?.map { castMember ->
                    CastMember(
                        id = castMember.id,
                        name = castMember.name,
                        character = castMember.character,
                        profileImageUrl = castMember.profilePath?.let {
                            "https://image.tmdb.org/t/p/w185$it"
                        }
                    )
                } ?: emptyList()

                val posterUrl = details?.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                val genres = details?.genres?.map { it.name } ?: emptyList()
                val runtime = details?.episodeRunTime?.firstOrNull()
                val runtimeText = runtime?.let { "$it min/episode" } ?: ""

                val isInWatchlist = withContext(Dispatchers.IO) {
                    watchlistRepository.isMovieInCurrentWatchlist(seriesId)
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    seriesId = seriesId,
                    title = details?.name,
                    overview = details?.overview,
                    posterUrl = posterUrl,
                    firstAirDate = details?.firstAirDate,
                    lastAirDate = details?.lastAirDate,
                    voteAverage = voteAverage,
                    genres = genres,
                    cast = cast,
                    numberOfSeasons = details?.numberOfSeasons ?: 0,
                    numberOfEpisodes = details?.numberOfEpisodes ?: 0,
                    runtime = runtimeText,
                    isInWatchlist = isInWatchlist
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Unknown error")
            }
        }
    }

    fun retry() {
        load()
    }

    fun toggleWatchlist() {
        val currentTitle = _uiState.value.title ?: return
        val isCurrentlyInWatchlist = _uiState.value.isInWatchlist

        viewModelScope.launch {
            try {
                if (isCurrentlyInWatchlist) {
                    removeFromCurrentWatchlist(seriesId, currentTitle)
                } else {
                    handleAddToWatchlist(seriesId, currentTitle)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Error updating watchlist: ${e.message}"
                )
            }
        }
    }

    private suspend fun removeFromCurrentWatchlist(seriesId: Long, seriesTitle: String) {
        val watchlist = withContext(Dispatchers.IO) {
            watchlistRepository.getCurrentWatchlist().first()
        }
        if (watchlist != null) {
            watchlistRepository.removeFromWatchlist(watchlist.id, seriesId)
            _uiState.value = _uiState.value.copy(
                isInWatchlist = false,
                snackbarMessage = "$seriesTitle removed from ${watchlist.name}"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Create a watchlist first"
            )
        }
    }

    private suspend fun handleAddToWatchlist(seriesId: Long, seriesTitle: String) {
        val watchlists = loadWatchlistsEnsuringDefault()
        if (watchlists.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Create a watchlist first"
            )
        } else {
            val current = watchlists.firstOrNull { it.isCurrent } ?: watchlists.first()
            addSeriesToWatchlist(current, seriesId, seriesTitle)
        }
    }

    private suspend fun loadWatchlistsEnsuringDefault(): List<WatchlistUiModel> {
        return withContext(Dispatchers.IO) {
            watchlistRepository.getAllWatchlists().first()
        }
    }

    private suspend fun addSeriesToWatchlist(
        watchlist: WatchlistUiModel,
        seriesId: Long,
        seriesTitle: String
    ) {
        watchlistRepository.setCurrentWatchlist(watchlist.id)
        watchlistRepository.addSeriesToWatchlist(watchlist.id, seriesId)
        _uiState.value = _uiState.value.copy(
            isInWatchlist = true,
            snackbarMessage = "$seriesTitle added to ${watchlist.name}"
        )
    }
    
    fun clearSnackbarMessage() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}

data class SeriesDetailsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val seriesId: Long = 0L,
    val title: String? = null,
    val overview: String? = null,
    val posterUrl: String? = null,
    val firstAirDate: String? = null,
    val lastAirDate: String? = null,
    val voteAverage: Double = 0.0,
    val genres: List<String> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val numberOfSeasons: Int = 0,
    val numberOfEpisodes: Int = 0,
    val runtime: String = "",
    val isInWatchlist: Boolean = false,
    val snackbarMessage: String? = null
)

