package com.example.cinephile.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.QuizRepository
import com.example.cinephile.domain.repository.QuizUiModel
import com.example.cinephile.domain.repository.QuizResultUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class QuizDetailsViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: Long = savedStateHandle.get<Long>("quizId") ?: 0L

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val quiz: StateFlow<QuizUiModel?> =
        flow {
            _isLoading.value = true
            _error.value = null
            try {
                val source = quizRepository.getQuiz(quizId)
                emitAll(source)
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = e.message ?: "Error loading quiz"
                emit(null)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val results: StateFlow<List<QuizResultUiModel>> =
        quizRepository.getQuizResults(quizId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val questionCount: StateFlow<Int> =
        flow {
            val source = quizRepository.getQuizQuestions(quizId)
            emitAll(source)
        }.map { it.size }
            .stateIn(viewModelScope, SharingStarted.Lazily, 0)
    
    fun retry() {
        _error.value = null
        _isLoading.value = true
        
    }
}

