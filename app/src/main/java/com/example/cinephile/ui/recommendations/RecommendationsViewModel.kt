package com.example.cinephile.ui.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.MovieRepository
import com.example.cinephile.domain.repository.RecommendationRepository
import com.example.cinephile.ui.search.MovieUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    private val recommendationRepository: RecommendationRepository,
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendationsUiState(isLoading = true))
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()

    init {
        observeCachedRecommendations()
        observeGenres()
        refreshRecommendations()
    }

    private fun observeCachedRecommendations() {
        viewModelScope.launch {
            recommendationRepository.getCachedRecommendations().collect { cached ->
                _uiState.update { current ->
                    val showCacheBadge = cached.isNotEmpty() && (current.recommendations.isEmpty() || current.isCached)
                    val sections = buildSections(cached)
                    current.copy(
                        latestMovies = sections.latest,
                        upcomingMovies = sections.upcoming,
                        recommendations = cached,
                        isCached = showCacheBadge,
                        errorMessage = if (cached.isEmpty() && !current.isLoading) current.errorMessage else null
                    )
                }
            }
        }
    }

    private fun observeGenres() {
        viewModelScope.launch {
            try {
                movieRepository.fetchAndCacheGenres()
            } catch (_: Exception) {
                // Ignore fetch failures; we'll still attempt to display cached genres
            }
            movieRepository.getGenresFlow().collect { genres ->
                val chips = genres
                    .sortedBy { it.name.lowercase() }
                    .take(MAX_GENRE_CHIPS)
                    .map { GenreChipUiModel(it.id, it.name) }
                _uiState.update { it.copy(genres = chips) }
            }
        }
    }

    fun refreshRecommendations(limit: Int = DEFAULT_LIMIT) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val fresh = recommendationRepository.computeRecommendations(limit)
                val sections = buildSections(fresh)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        latestMovies = sections.latest,
                        upcomingMovies = sections.upcoming,
                        recommendations = fresh,
                        isCached = false,
                        errorMessage = null
                    )
                }
            } catch (throwable: Throwable) {
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: DEFAULT_ERROR
                    )
                }
            }
        }
    }

    data class RecommendationsUiState(
        val isLoading: Boolean = false,
        val latestMovies: List<MovieUiModel> = emptyList(),
        val upcomingMovies: List<MovieUiModel> = emptyList(),
        val recommendations: List<MovieUiModel> = emptyList(),
        val genres: List<GenreChipUiModel> = emptyList(),
        val isCached: Boolean = false,
        val errorMessage: String? = null
    )

    data class GenreChipUiModel(
        val id: Int,
        val name: String
    )

    private data class Sections(
        val latest: List<MovieUiModel> = emptyList(),
        val upcoming: List<MovieUiModel> = emptyList()
    )

    private fun buildSections(movies: List<MovieUiModel>): Sections {
        if (movies.isEmpty()) return Sections()
        val today = runCatching { LocalDate.now() }.getOrNull()
        val latest = mutableListOf<MovieUiModel>()
        val upcoming = mutableListOf<MovieUiModel>()

        movies.forEach { movie ->
            val releaseDate = movie.releaseDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            when {
                releaseDate == null || today == null -> latest += movie
                releaseDate.isAfter(today) -> upcoming += movie
                else -> latest += movie
            }
        }

        val latestSorted = latest
            .distinctBy { it.id }
            .sortedByDescending { it.releaseDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } }
            .take(MAX_SECTION_ITEMS)

        val upcomingSorted = upcoming
            .distinctBy { it.id }
            .sortedBy { it.releaseDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } }
            .take(MAX_SECTION_ITEMS)

        return Sections(latestSorted, upcomingSorted)
    }

    companion object {
        private const val DEFAULT_LIMIT = 20
        private const val DEFAULT_ERROR = "Unable to load recommendations."
        private const val MAX_SECTION_ITEMS = 10
        private const val MAX_GENRE_CHIPS = 12
    }
}
