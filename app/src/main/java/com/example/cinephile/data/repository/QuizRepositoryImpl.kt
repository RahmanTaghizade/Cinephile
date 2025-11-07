package com.example.cinephile.data.repository

import com.example.cinephile.data.local.dao.QuizDao
import com.example.cinephile.data.local.dao.WatchlistDao
import com.example.cinephile.data.local.entities.QuizEntity
import com.example.cinephile.domain.repository.QuizRepository
import com.example.cinephile.domain.repository.QuizUiModel
import com.example.cinephile.domain.repository.QuizQuestionUiModel
import com.example.cinephile.domain.repository.QuizResultUiModel
import com.example.cinephile.domain.repository.QuizDifficulty
import com.example.cinephile.domain.repository.QuizMode
import com.example.cinephile.domain.repository.QuizGenerationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val quizDao: QuizDao,
    private val watchlistDao: WatchlistDao
) : QuizRepository {

    override fun getAllQuizzes(): Flow<List<QuizUiModel>> {
        return quizDao.listQuizzes().flatMapLatest { quizzes ->
            flow {
                val watchlistIds = quizzes.map { it.watchlistId }.distinct()
                val watchlistMap = watchlistIds.associateWith { id ->
                    watchlistDao.getById(id)?.name ?: "Unknown"
                }
                
                emit(quizzes.map { quiz ->
                    QuizUiModel(
                        id = quiz.id,
                        name = quiz.name,
                        watchlistName = watchlistMap[quiz.watchlistId] ?: "Unknown",
                        createdAt = quiz.createdAt,
                        difficulty = when (quiz.difficulty) {
                            "Easy" -> QuizDifficulty.EASY
                            "Medium" -> QuizDifficulty.MEDIUM
                            "Hard" -> QuizDifficulty.HARD
                            else -> QuizDifficulty.EASY
                        },
                        mode = when (quiz.mode) {
                            "Timed" -> QuizMode.TIMED
                            "Survival" -> QuizMode.SURVIVAL
                            else -> QuizMode.TIMED
                        },
                        questionCount = quiz.questionCount
                    )
                })
            }
        }
    }

    override suspend fun createQuiz(name: String, watchlistId: Long, questionCount: Int, difficulty: QuizDifficulty, mode: QuizMode): Long {
        val quizEntity = QuizEntity(
            name = name,
            watchlistId = watchlistId,
            createdAt = System.currentTimeMillis(),
            difficulty = when (difficulty) {
                QuizDifficulty.EASY -> "Easy"
                QuizDifficulty.MEDIUM -> "Medium"
                QuizDifficulty.HARD -> "Hard"
            },
            mode = when (mode) {
                QuizMode.TIMED -> "Timed"
                QuizMode.SURVIVAL -> "Survival"
            },
            questionCount = questionCount
        )
        return quizDao.insertQuiz(quizEntity)
    }

    override fun getQuiz(quizId: Long): Flow<QuizUiModel?> {
        // Stub implementation - return null for now
        return flowOf(null)
    }

    override suspend fun generateQuestions(quizId: Long): QuizGenerationResult {
        // Stub implementation - return dummy result for now
        return QuizGenerationResult(
            success = true,
            questionsGenerated = 5,
            message = "Questions generated successfully (stub)"
        )
    }

    override fun getQuizQuestions(quizId: Long): Flow<List<QuizQuestionUiModel>> {
        // Stub implementation - return empty list for now
        return flowOf(emptyList())
    }

    override suspend fun saveQuizResult(quizId: Long, score: Int, durationSec: Int, correctCount: Int, wrongCount: Int, mode: QuizMode) {
        // Stub implementation - no-op for now
    }

    override fun getQuizResults(quizId: Long): Flow<List<QuizResultUiModel>> {
        // Stub implementation - return empty list for now
        return flowOf(emptyList())
    }
}
