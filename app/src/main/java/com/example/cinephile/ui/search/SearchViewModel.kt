package com.example.cinephile.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.MovieRepository
import com.example.cinephile.domain.repository.MovieFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.cinephile.data.local.entities.GenreEntity
import kotlinx.coroutines.flow.collect
import com.example.cinephile.data.remote.TmdbPerson

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _searchUiState = MutableStateFlow(SearchUiState())
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()
    
    private var searchJob: Job? = null
    private var currentPage = 1
    private var totalPages = 1
    private var isLoadingPage = false

    private val _genres = MutableStateFlow<List<GenreEntity>>(emptyList())
    val genres: StateFlow<List<GenreEntity>> = _genres.asStateFlow()

    private val _selectedGenreIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedGenreIds: StateFlow<Set<Int>> = _selectedGenreIds.asStateFlow()

    private val _actorSuggestions = MutableStateFlow<List<TmdbPerson>>(emptyList())
    val actorSuggestions: StateFlow<List<TmdbPerson>> = _actorSuggestions.asStateFlow()
    private val _directorSuggestions = MutableStateFlow<List<TmdbPerson>>(emptyList())
    val directorSuggestions: StateFlow<List<TmdbPerson>> = _directorSuggestions.asStateFlow()

    private val _selectedActors = MutableStateFlow<List<TmdbPerson>>(emptyList())
    val selectedActors: StateFlow<List<TmdbPerson>> = _selectedActors.asStateFlow()
    private val _selectedDirectors = MutableStateFlow<List<TmdbPerson>>(emptyList())
    val selectedDirectors: StateFlow<List<TmdbPerson>> = _selectedDirectors.asStateFlow()

    private var actorSearchJob: Job? = null
    private var directorSearchJob: Job? = null

    fun onQueryChanged(query: String) {
        savedStateHandle["query"] = query
        _searchUiState.value = _searchUiState.value.copy(query = query)
        
        // Cancel previous search
        searchJob?.cancel()
        
        // Debounce search with 400ms delay
        searchJob = viewModelScope.launch {
            delay(400)
            if (query.trim().isNotEmpty() || hasActiveFilters()) {
                currentPage = 1
                performSearch()
            }
        }
    }
    
    fun onSearchClick() {
        currentPage = 1
        performSearch()
    }

    fun loadNextPage() {
        if (currentPage < totalPages && !isLoadingPage) {
            isLoadingPage = true
            performSearch(isLoadMore = true)
        }
    }
    
    private fun hasActiveFilters(): Boolean {
        val state = _searchUiState.value
        return state.selectedYear != null || 
               _selectedGenreIds.value.isNotEmpty() ||
               _selectedActors.value.isNotEmpty() ||
               _selectedDirectors.value.isNotEmpty()
    }

    private fun buildFilters(): MovieFilters {
        val query = _searchUiState.value.query.trim().takeIf { it.isNotEmpty() }
        val selectedYear = _searchUiState.value.selectedYear
        val genreIds = _selectedGenreIds.value.toList()
        val actorIds = _selectedActors.value.map { it.id }
        val directorIds = _selectedDirectors.value.map { it.id }
        
        return MovieFilters(
            query = query,
            primaryReleaseYear = selectedYear,
            genreIds = genreIds,
            actorIds = actorIds,
            directorIds = directorIds
        )
    }
    
    private fun performSearch(isLoadMore: Boolean = false) {
        viewModelScope.launch {
            try {
                if (!isLoadMore) {
                    _searchUiState.value = _searchUiState.value.copy(
                        isLoading = true,
                        error = null,
                        isOffline = false,
                        cacheTimestamp = null
                    )
                }
                
                val filters = buildFilters()
                val result = movieRepository.searchMovies(filters, currentPage)
                
                currentPage = result.currentPage
                totalPages = result.totalPages
                isLoadingPage = false
                
                if (isLoadMore) {
                    _searchUiState.value = _searchUiState.value.copy(
                        movies = _searchUiState.value.movies + result.movies
                    )
                } else {
                    _searchUiState.value = _searchUiState.value.copy(
                        movies = result.movies,
                        isLoading = false,
                        isOffline = result.isFromCache,
                        cacheTimestamp = result.cacheTimestamp
                    )
                }
            } catch (e: Exception) {
                isLoadingPage = false
                _searchUiState.value = _searchUiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    fun clearError() {
        _searchUiState.value = _searchUiState.value.copy(error = null)
    }

    fun onActorSearchQueryChanged(query: String) {
        actorSearchJob?.cancel()
        actorSearchJob = viewModelScope.launch {
            delay(300)
            if (query.isNotBlank()) {
                try {
                    val response = movieRepository.tmdbService.searchPerson(query)
                    _actorSuggestions.value = response.results
                } catch (_: Exception) {}
            } else {
                _actorSuggestions.value = emptyList()
            }
        }
    }

    fun onDirectorSearchQueryChanged(query: String) {
        directorSearchJob?.cancel()
        directorSearchJob = viewModelScope.launch {
            delay(300)
            if (query.isNotBlank()) {
                try {
                    val response = movieRepository.tmdbService.searchPerson(query)
                    _directorSuggestions.value = response.results
                } catch (_: Exception) {}
            } else {
                _directorSuggestions.value = emptyList()
            }
        }
    }

    fun addSelectedActor(person: TmdbPerson) {
        if (!_selectedActors.value.any { it.id == person.id }) {
            _selectedActors.value = _selectedActors.value + person
        }
    }
    fun removeSelectedActor(personId: Long) {
        _selectedActors.value = _selectedActors.value.filterNot { it.id == personId }
    }
    fun addSelectedDirector(person: TmdbPerson) {
        if (!_selectedDirectors.value.any { it.id == person.id }) {
            _selectedDirectors.value = _selectedDirectors.value + person
        }
    }
    fun removeSelectedDirector(personId: Long) {
        _selectedDirectors.value = _selectedDirectors.value.filterNot { it.id == personId }
    }

    init {
        // Restore saved query
        savedStateHandle.get<String>("query")?.let { query ->
            _searchUiState.value = _searchUiState.value.copy(query = query)
        }
        // Fetch & observe genres
        viewModelScope.launch {
            movieRepository.fetchAndCacheGenres()
            movieRepository.getGenresFlow().collect { list ->
                _genres.value = list
            }
        }
    }

    fun onGenreChipClicked(id: Int) {
        val current = _selectedGenreIds.value
        _selectedGenreIds.value = if (id in current)
            current - id else current + id
        // Trigger search on filter change
        onSearchClick()
    }
}

data class SearchUiState(
    val query: String = "",
    val movies: List<MovieUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedYear: Int? = null,
    val isOffline: Boolean = false,
    val cacheTimestamp: Long? = null
)
