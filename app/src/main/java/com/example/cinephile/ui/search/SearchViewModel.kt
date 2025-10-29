package com.example.cinephile.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _searchUiState = MutableStateFlow(SearchUiState())
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()
    
    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        savedStateHandle["query"] = query
        _searchUiState.value = _searchUiState.value.copy(query = query)
        
        // Cancel previous search
        searchJob?.cancel()
        
        // Debounce search with 400ms delay
        searchJob = viewModelScope.launch {
            delay(400)
            if (query.trim().isNotEmpty()) {
                performSearch(query.trim())
            }
        }
    }
    
    fun onSearchClick() {
        val query = _searchUiState.value.query.trim()
        if (query.isNotEmpty()) {
            performSearch(query)
        }
    }
    
    private fun performSearch(query: String) {
        viewModelScope.launch {
            try {
                _searchUiState.value = _searchUiState.value.copy(
                    isLoading = true,
                    error = null
                )
                
                // Get the first emission from the flow
                val movies = movieRepository.searchMovies(query).first()
                
                _searchUiState.value = _searchUiState.value.copy(
                    movies = movies,
                    isLoading = false
                )
            } catch (e: Exception) {
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

    init {
        // Restore saved query
        savedStateHandle.get<String>("query")?.let { query ->
            _searchUiState.value = _searchUiState.value.copy(query = query)
        }
    }
}

data class SearchUiState(
    val query: String = "",
    val movies: List<MovieUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
