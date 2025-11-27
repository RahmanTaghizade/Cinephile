package com.example.cinephile.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.QuizRepository
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
                questionCount = quiz.questionCount
            )
        }
    }

    suspend fun createQuiz(
        name: String,
        watchlistId: Long,
        questionCount: Int,
        difficulty: com.example.cinephile.domain.repository.QuizDifficulty = com.example.cinephile.domain.repository.QuizDifficulty.EASY,
        mode: com.example.cinephile.domain.repository.QuizMode = com.example.cinephile.domain.repository.QuizMode.TIMED
    ): Long {
        val quizId = quizRepository.createQuiz(name, watchlistId, questionCount, difficulty, mode)
        
        quizRepository.generateQuestions(quizId)
        return quizId
    }
}

data class QuizListItem(
    val id: Long,
    val name: String,
    val watchlistName: String,
    val createdAt: Long,
    val questionCount: Int
)

