package com.example.cinephile.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.MovieRepository
import com.example.cinephile.domain.repository.WatchlistRepository
import com.example.cinephile.domain.repository.WatchlistUiModel
import com.example.cinephile.ui.search.MovieUiModel
import com.example.cinephile.ui.search.CastMember
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import com.example.cinephile.data.remote.TmdbService

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val watchlistRepository: WatchlistRepository,
    private val tmdbService: TmdbService,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DetailsEvent>()
    val events: SharedFlow<DetailsEvent> = _events.asSharedFlow()

    private val movieId: Long = checkNotNull(savedStateHandle["movieId"]) { "movieId is required" }

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Fetch cast and rating from API
                val details = kotlin.runCatching { tmdbService.getMovie(movieId) }.getOrNull()
                val credits = kotlin.runCatching { tmdbService.getCredits(movieId) }.getOrNull()
                
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
                
                movieRepository.getMovieDetails(movieId).collect { movie ->
                    val posterUrl = movie?.posterUrl
                    // Check if movie is in current watchlist
                    val isInWatchlist = movie?.let { 
                        watchlistRepository.isMovieInCurrentWatchlist(it.id) 
                    } ?: false
                    
                    // Merge movie data with cast and rating
                    val movieWithDetails = movie?.copy(
                        voteAverage = voteAverage,
                        cast = cast
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        movie = movieWithDetails,
                        posterUrl = posterUrl,
                        title = movie?.title,
                        releaseDate = movie?.releaseDate,
                        director = movie?.director,
                        isFavorite = movie?.isFavorite ?: false,
                        userRating = movie?.userRating ?: 0f,
                        isInWatchlist = isInWatchlist
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleFavorite() {
        val current = _uiState.value.movie ?: return
        val newFavorite = !current.isFavorite
        // optimistic update
        _uiState.value = _uiState.value.copy(
            movie = current.copy(isFavorite = newFavorite),
            isFavorite = newFavorite
        )
        viewModelScope.launch {
            val result = movieRepository.toggleFavorite(current.id, newFavorite)
            if (result == null) {
                // rollback on failure
                _uiState.value = _uiState.value.copy(
                    movie = current,
                    isFavorite = current.isFavorite
                )
            }
        }
    }

    fun setRating(rating: Float) {
        val current = _uiState.value.movie ?: return
        // optimistic update
        _uiState.value = _uiState.value.copy(
            movie = current.copy(userRating = rating),
            userRating = rating
        )
        viewModelScope.launch {
            val result = movieRepository.rateMovie(current.id, rating)
            if (result == null) {
                // rollback on failure
                _uiState.value = _uiState.value.copy(
                    movie = current,
                    userRating = current.userRating
                )
            }
        }
    }

    fun toggleWatchlist() {
        val current = _uiState.value.movie ?: return
        val isCurrentlyInWatchlist = _uiState.value.isInWatchlist

        viewModelScope.launch {
            try {
                if (isCurrentlyInWatchlist) {
                    removeFromCurrentWatchlist(current.id, current.title)
                } else {
                    handleAddToWatchlist(current.id, current.title)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Error updating watchlist: ${e.message}"
                )
            }
        }
    }

    private suspend fun removeFromCurrentWatchlist(movieId: Long, movieTitle: String) {
        val watchlist = withContext(Dispatchers.IO) {
            watchlistRepository.getCurrentWatchlist().first()
        }
        if (watchlist != null) {
            watchlistRepository.removeFromWatchlist(watchlist.id, movieId)
            _uiState.value = _uiState.value.copy(
                isInWatchlist = false,
                snackbarMessage = "$movieTitle removed from ${watchlist.name}"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Create a watchlist first"
            )
        }
    }

    private suspend fun handleAddToWatchlist(movieId: Long, movieTitle: String) {
        val watchlists = loadWatchlistsEnsuringDefault()
        if (watchlists.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Create a watchlist first"
            )
        } else {
            _events.emit(DetailsEvent.ShowWatchlistPicker(watchlists))
        }
    }

    private suspend fun loadWatchlistsEnsuringDefault(): List<WatchlistUiModel> {
        var watchlists = withContext(Dispatchers.IO) {
            watchlistRepository.getAllWatchlists().first()
        }
        if (watchlists.isEmpty()) {
            val id = watchlistRepository.createWatchlist("My Watchlist")
            watchlistRepository.setCurrentWatchlist(id)
            watchlists = withContext(Dispatchers.IO) {
                watchlistRepository.getAllWatchlists().first()
            }
        }
        return watchlists
    }

    private suspend fun addMovieToWatchlist(
        watchlist: WatchlistUiModel,
        movieId: Long,
        movieTitle: String
    ) {
        watchlistRepository.setCurrentWatchlist(watchlist.id)
        watchlistRepository.addToWatchlist(watchlist.id, movieId)
        _uiState.value = _uiState.value.copy(
            isInWatchlist = true,
            snackbarMessage = "$movieTitle added to ${watchlist.name}"
        )
    }

    fun confirmAddToWatchlist(watchlistId: Long) {
        val movie = _uiState.value.movie ?: return
        viewModelScope.launch {
            try {
                val watchlist = withContext(Dispatchers.IO) {
                    watchlistRepository.getAllWatchlists()
                        .first()
                        .firstOrNull { it.id == watchlistId }
                }
                if (watchlist != null) {
                    addMovieToWatchlist(watchlist, movie.id, movie.title)
                } else {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Watchlist no longer exists"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Error updating watchlist: ${e.message}"
                )
            }
        }
    }

    fun createWatchlistAndAdd(name: String) {
        val movie = _uiState.value.movie ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Please enter a watchlist name"
            )
            return
        }
        viewModelScope.launch {
            try {
                val id = withContext(Dispatchers.IO) {
                    watchlistRepository.createWatchlist(trimmed)
                }
                val watchlist = WatchlistUiModel(
                    id = id,
                    name = trimmed,
                    isCurrent = true
                )
                addMovieToWatchlist(watchlist, movie.id, movie.title)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Error creating watchlist: ${e.message}"
                )
            }
        }
    }
    
    fun clearSnackbarMessage() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}

data class DetailsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val movie: MovieUiModel? = null,
    val posterUrl: String? = null,
    val title: String? = null,
    val releaseDate: String? = null,
    val director: String? = null,
    val isFavorite: Boolean = false,
    val userRating: Float = 0f,
    val isInWatchlist: Boolean = false,
    val snackbarMessage: String? = null
)

sealed class DetailsEvent {
    data class ShowWatchlistPicker(val watchlists: List<WatchlistUiModel>) : DetailsEvent()
}


