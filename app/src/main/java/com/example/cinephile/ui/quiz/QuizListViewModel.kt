package com.example.cinephile.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.QuizRepository
import com.example.cinephile.domain.repository.QuizDifficulty
import com.example.cinephile.domain.repository.QuizMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizListViewModel @Inject constructor(
    private val quizRepository: QuizRepository
) : ViewModel() {

    val quizzes: Flow<List<QuizListItem>> = quizRepository.getAllQuizzes().map { list ->
        list.map { quiz ->
            QuizListItem(
                id = quiz.id,
                name = quiz.name,
                watchlistName = quiz.watchlistName,
                createdAt = quiz.createdAt,
                difficulty = quiz.difficulty,
                mode = quiz.mode,
                questionCount = quiz.questionCount
            )
        }
    }

    suspend fun createQuiz(
        name: String,
        watchlistId: Long,
        questionCount: Int,
        difficulty: QuizDifficulty,
        mode: QuizMode
    ): Long {
        return quizRepository.createQuiz(name, watchlistId, questionCount, difficulty, mode)
    }
}

data class QuizListItem(
    val id: Long,
    val name: String,
    val watchlistName: String,
    val createdAt: Long,
    val difficulty: QuizDifficulty,
    val mode: QuizMode,
    val questionCount: Int
)

