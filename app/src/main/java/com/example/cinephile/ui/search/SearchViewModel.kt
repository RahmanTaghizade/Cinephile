package com.example.cinephile.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow("Hello from SearchViewModel with MovieRepository injected!")
    val uiState: StateFlow<String> = _uiState.asStateFlow()

    init {
        // Test that MovieRepository is properly injected
        viewModelScope.launch {
            try {
                // This is just a test call to ensure the repository is working
                // We'll implement actual search functionality later
                val helloMessage = movieRepository.getHelloMessage()
                _uiState.value = helloMessage
            } catch (e: Exception) {
                _uiState.value = "Error: ${e.message}"
            }
        }
    }
}
