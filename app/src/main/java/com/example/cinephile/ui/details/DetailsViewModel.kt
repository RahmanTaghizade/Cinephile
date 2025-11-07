package com.example.cinephile.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.MovieRepository
import com.example.cinephile.domain.repository.WatchlistRepository
import com.example.cinephile.ui.search.MovieUiModel
import com.example.cinephile.ui.search.CastMember
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
                val currentWatchlist = watchlistRepository.getCurrentWatchlist().first()
                currentWatchlist?.let { watchlist ->
                    if (isCurrentlyInWatchlist) {
                        // Remove from watchlist
                        watchlistRepository.removeFromWatchlist(watchlist.id, current.id)
                        _uiState.value = _uiState.value.copy(
                            isInWatchlist = false,
                            snackbarMessage = "${current.title} removed from watchlist"
                        )
                    } else {
                        // Add to watchlist
                        watchlistRepository.addToWatchlist(watchlist.id, current.id)
                        _uiState.value = _uiState.value.copy(
                            isInWatchlist = true,
                            snackbarMessage = "${current.title} added to watchlist"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Error updating watchlist: ${e.message}"
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


