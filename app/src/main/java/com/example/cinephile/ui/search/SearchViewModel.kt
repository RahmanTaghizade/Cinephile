package com.example.cinephile.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.cinephile.domain.repository.MovieRepository
import com.example.cinephile.domain.repository.MovieFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject
import com.example.cinephile.data.local.entities.GenreEntity
import kotlinx.coroutines.flow.collect
import com.example.cinephile.data.remote.TmdbPerson
import com.example.cinephile.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val watchlistRepository: WatchlistRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val TAG = "SearchVM"

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

    // UI event for showing snackbars/actions (single-use)
    private val _uiEvent = MutableSharedFlow<SearchUiEvent>()
    val uiEvent: SharedFlow<SearchUiEvent> = _uiEvent

    private var lastAdded: Pair<Long, Long>? = null // watchlistId to movieId

    fun onQueryChanged(query: String) {
        savedStateHandle["query"] = query
        _searchUiState.value = _searchUiState.value.copy(query = query)
        // Save filter state
        savedStateHandle["selectedYear"] = _searchUiState.value.selectedYear
        savedStateHandle["selectedGenreIds"] = _selectedGenreIds.value
        savedStateHandle["selectedActorIds"] = _selectedActors.value.map { it.id }
        savedStateHandle["selectedDirectorIds"] = _selectedDirectors.value.map { it.id }
        
        // Cancel previous search
        searchJob?.cancel()
        
        // Debounce search with 400ms delay
        searchJob = viewModelScope.launch {
            delay(400)
            if (query.trim().isNotEmpty() || hasActiveFilters()) {
                Log.d(TAG, "Debounced query change -> triggering search; query='${query.trim()}'")
                currentPage = 1
                performSearch()
            }
        }
    }
    
    fun onSearchClick() {
        Log.d(TAG, "Search button clicked with query='${_searchUiState.value.query.trim()}' and filters year=${_searchUiState.value.selectedYear} genres=${_selectedGenreIds.value} actors=${_selectedActors.value.map { it.id }} directors=${_selectedDirectors.value.map { it.id }}")
        currentPage = 1
        performSearch()
    }

    fun applyGenreFromHome(genreId: Int, genreName: String?) {
        val currentGenres = _selectedGenreIds.value
        if (currentGenres.size == 1 && currentGenres.contains(genreId) &&
            _searchUiState.value.activeGenreName == genreName
        ) {
            onSearchClick()
            return
        }

        _selectedGenreIds.value = setOf(genreId)
        savedStateHandle["selectedGenreIds"] = _selectedGenreIds.value
        _searchUiState.value = _searchUiState.value.copy(activeGenreName = genreName)
        onSearchClick()
    }

    fun loadNextPage() {
        if (currentPage < totalPages && !isLoadingPage) {
            Log.d(TAG, "Loading next page: current=$currentPage total=$totalPages")
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
                Log.d(TAG, "performSearch start | page=$currentPage isLoadMore=$isLoadMore | filters=$filters")

                // Run movies and people search concurrently
                val movieResult = movieRepository.searchMovies(filters, currentPage)
                val people = if (_searchUiState.value.query.isNotBlank()) {
                    kotlin.runCatching { movieRepository.tmdbService.searchPerson(_searchUiState.value.query).results }
                        .getOrDefault(emptyList())
                } else emptyList()
                _lastPeopleResults = people

                currentPage = movieResult.currentPage
                totalPages = movieResult.totalPages
                isLoadingPage = false

                val combined = buildCombinedResults(movieResult.movies, people)

                _searchUiState.value = _searchUiState.value.copy(
                    movies = movieResult.movies,
                    results = applyFilter(combined, _searchUiState.value.activeFilter),
                    isLoading = false,
                    isOffline = movieResult.isFromCache,
                    cacheTimestamp = movieResult.cacheTimestamp
                )
                Log.d(TAG, "performSearch success | results=${combined.size}")
            } catch (e: Exception) {
                isLoadingPage = false
                _searchUiState.value = _searchUiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
                Log.e(TAG, "performSearch failed", e)
            }
        }
    }

    private fun buildCombinedResults(
        movies: List<MovieUiModel>,
        people: List<TmdbPerson>
    ): List<SearchResult> {
        val movieItems = movies.map { SearchResult.MovieItem(it) }
        val personItems = people.map { SearchResult.PersonItem(it) }
        return movieItems + personItems
    }

    fun onFilterSelected(filter: SearchFilter) {
        _searchUiState.value = _searchUiState.value.copy(activeFilter = filter)
        val combined = buildCombinedResults(_searchUiState.value.movies, _lastPeopleResults)
        _searchUiState.value = _searchUiState.value.copy(results = applyFilter(combined, filter))
    }

    private var _lastPeopleResults: List<TmdbPerson> = emptyList()

    private fun applyFilter(all: List<SearchResult>, filter: SearchFilter): List<SearchResult> {
        return when (filter) {
            SearchFilter.ALL -> all
            SearchFilter.MOVIES -> all.filterIsInstance<SearchResult.MovieItem>()
            SearchFilter.ACTORS -> all.filterIsInstance<SearchResult.PersonItem>()
                .filter { it.person.knownForDepartment.equals("Acting", true) }
            SearchFilter.PRODUCERS -> all.filterIsInstance<SearchResult.PersonItem>()
                .filter { it.person.knownForDepartment.contains("Production", true) || it.person.knownForDepartment.contains("Producer", true) }
            SearchFilter.CARTOONS -> all.filterIsInstance<SearchResult.MovieItem>()
                .filter { item -> item.movie.genres.any { it.contains("Animation", true) } }
            SearchFilter.SERIES -> emptyList() // Not implemented in data layer yet
        }
    }
    
    fun clearError() {
        _searchUiState.value = _searchUiState.value.copy(error = null)
    }

    fun clearActiveGenre() {
        if (_selectedGenreIds.value.isEmpty() && _searchUiState.value.activeGenreName == null) return

        _selectedGenreIds.value = emptySet()
        savedStateHandle["selectedGenreIds"] = _selectedGenreIds.value
        _searchUiState.value = _searchUiState.value.copy(activeGenreName = null)

        if (_searchUiState.value.query.isNotBlank()) {
            onSearchClick()
        } else {
            currentPage = 1
            totalPages = 1
            _searchUiState.value = _searchUiState.value.copy(
                movies = emptyList(),
                results = emptyList(),
                isLoading = false,
                error = null,
                isOffline = false,
                cacheTimestamp = null
            )
        }
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
            savedStateHandle["selectedActorIds"] = _selectedActors.value.map { it.id }
        }
    }
    fun removeSelectedActor(personId: Long) {
        _selectedActors.value = _selectedActors.value.filterNot { it.id == personId }
        savedStateHandle["selectedActorIds"] = _selectedActors.value.map { it.id }
    }
    fun addSelectedDirector(person: TmdbPerson) {
        if (!_selectedDirectors.value.any { it.id == person.id }) {
            _selectedDirectors.value = _selectedDirectors.value + person
            savedStateHandle["selectedDirectorIds"] = _selectedDirectors.value.map { it.id }
        }
    }
    fun removeSelectedDirector(personId: Long) {
        _selectedDirectors.value = _selectedDirectors.value.filterNot { it.id == personId }
        savedStateHandle["selectedDirectorIds"] = _selectedDirectors.value.map { it.id }
    }

    fun addToCurrentWatchlist(movieId: Long) {
        viewModelScope.launch {
            try {
                val current = watchlistRepository.getCurrentWatchlist().first()
                if (current != null) {
                    watchlistRepository.addToWatchlist(current.id, movieId)
                    lastAdded = current.id to movieId
                    _uiEvent.emit(SearchUiEvent.ShowAddedToWatchlist(movieId))
                } else {
                    // No current watchlist set; optionally emit a different event/message
                    _uiEvent.emit(SearchUiEvent.ShowUndoFailed)
                }
            } catch (_: Exception) {
                _uiEvent.emit(SearchUiEvent.ShowUndoFailed)
            }
        }
    }

    fun undoAdd() {
        viewModelScope.launch {
            val (watchlistId, movieId) = lastAdded ?: return@launch
            try {
                watchlistRepository.removeFromWatchlist(watchlistId, movieId)
                lastAdded = null
            } catch (_: Exception) {
                _uiEvent.emit(SearchUiEvent.ShowUndoFailed)
            }
        }
    }

    init {
        // Restore saved query
        savedStateHandle.get<String>("query")?.let { query ->
            _searchUiState.value = _searchUiState.value.copy(query = query)
        }
        // Restore selected year
        savedStateHandle.get<Int>("selectedYear")?.let { year ->
            _searchUiState.value = _searchUiState.value.copy(selectedYear = year)
        }
        // Restore selected genre IDs
        savedStateHandle.get<Set<Int>>("selectedGenreIds")?.let { ids ->
            _selectedGenreIds.value = ids
        }
        // Restore selected actors (serialized as List<Long> IDs)
        savedStateHandle.get<List<Long>>("selectedActorIds")?.let { actorIds ->
            // Will be restored when genres are loaded
        }
        // Restore selected directors (serialized as List<Long> IDs)
        savedStateHandle.get<List<Long>>("selectedDirectorIds")?.let { directorIds ->
            // Will be restored when genres are loaded
        }
        // Fetch & observe genres
        viewModelScope.launch {
            movieRepository.fetchAndCacheGenres()
            movieRepository.getGenresFlow().collect { list ->
                _genres.value = list
            }
        }

        // Observe user flags (favorite/rating) and update current list items optimistically
        viewModelScope.launch {
            movieRepository.observeUserFlags().collectLatest { flagsList ->
                if (flagsList.isEmpty()) return@collectLatest
                val flagsMap = flagsList.associateBy { it.id }
                val current = _searchUiState.value.movies
                if (current.isEmpty()) return@collectLatest
                val updated = current.map { item ->
                    val f = flagsMap[item.id]
                    if (f != null && (f.isFavorite != item.isFavorite || f.userRating != item.userRating)) {
                        item.copy(isFavorite = f.isFavorite, userRating = f.userRating)
                    } else item
                }
                if (updated !== current) {
                    _searchUiState.value = _searchUiState.value.copy(movies = updated)
                }
            }
        }
    }

    fun onGenreChipClicked(id: Int) {
        val current = _selectedGenreIds.value
        _selectedGenreIds.value = if (id in current)
            current - id else current + id
        savedStateHandle["selectedGenreIds"] = _selectedGenreIds.value
        // Trigger search on filter change
        onSearchClick()
    }
    
    fun setSelectedYear(year: Int?) {
        _searchUiState.value = _searchUiState.value.copy(selectedYear = year)
        savedStateHandle["selectedYear"] = year
    }
}

data class SearchUiState(
    val query: String = "",
    val movies: List<MovieUiModel> = emptyList(),
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedYear: Int? = null,
    val activeGenreName: String? = null,
    val isOffline: Boolean = false,
    val cacheTimestamp: Long? = null,
    val activeFilter: SearchFilter = SearchFilter.ALL
)

sealed class SearchUiEvent {
    data class ShowAddedToWatchlist(val movieId: Long): SearchUiEvent()
    object ShowUndoFailed: SearchUiEvent()
}
