package com.example.cinephile.data.repository

import com.example.cinephile.data.local.dao.QuizDao
import com.example.cinephile.domain.repository.QuizRepository
import com.example.cinephile.domain.repository.QuizUiModel
import com.example.cinephile.domain.repository.QuizQuestionUiModel
import com.example.cinephile.domain.repository.QuizResultUiModel
import com.example.cinephile.domain.repository.QuizDifficulty
import com.example.cinephile.domain.repository.QuizMode
import com.example.cinephile.domain.repository.QuizGenerationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val quizDao: QuizDao
) : QuizRepository {

    override suspend fun getAllQuizzes(): Flow<List<QuizUiModel>> {
        // Stub implementation - return empty list for now
        return flowOf(emptyList())
    }

    override suspend fun createQuiz(name: String, watchlistId: Long, questionCount: Int, difficulty: QuizDifficulty, mode: QuizMode): Long {
        // Stub implementation - return dummy ID for now
        return 1L
    }

    override suspend fun getQuiz(quizId: Long): Flow<QuizUiModel?> {
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

    override suspend fun getQuizQuestions(quizId: Long): Flow<List<QuizQuestionUiModel>> {
        // Stub implementation - return empty list for now
        return flowOf(emptyList())
    }

    override suspend fun saveQuizResult(quizId: Long, score: Int, durationSec: Int, correctCount: Int, wrongCount: Int, mode: QuizMode) {
        // Stub implementation - no-op for now
    }

    override suspend fun getQuizResults(quizId: Long): Flow<List<QuizResultUiModel>> {
        // Stub implementation - return empty list for now
        return flowOf(emptyList())
    }
}
