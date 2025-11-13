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

    val quiz: StateFlow<QuizUiModel?> =
        flow {
            val source = quizRepository.getQuiz(quizId)
            emitAll(source)
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val results: StateFlow<List<QuizResultUiModel>> =
        flow {
            val source = quizRepository.getQuizResults(quizId)
            emitAll(source)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val questionCount: StateFlow<Int> =
        flow {
            val source = quizRepository.getQuizQuestions(quizId)
            emitAll(source)
        }.map { it.size }
            .stateIn(viewModelScope, SharingStarted.Lazily, 0)
}

