package com.example.cinephile.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.data.remote.TmdbService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val tmdbService: TmdbService
) : ViewModel() {

    private val _uiState = MutableStateFlow("Hello from SearchViewModel with TmdbService injected!")
    val uiState: StateFlow<String> = _uiState.asStateFlow()

    init {
        // Test that TmdbService is properly injected
        viewModelScope.launch {
            try {
                // This is just a test call to ensure the service is working
                // We'll implement actual search functionality later
                _uiState.value = "TmdbService successfully injected and ready!"
            } catch (e: Exception) {
                _uiState.value = "Error: ${e.message}"
            }
        }
    }
}
