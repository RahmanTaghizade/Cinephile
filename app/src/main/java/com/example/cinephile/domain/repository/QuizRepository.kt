package com.example.cinephile.domain.repository

import kotlinx.coroutines.flow.Flow

interface QuizRepository {
    fun getAllQuizzes(): Flow<List<QuizUiModel>>
    suspend fun createQuiz(name: String, watchlistId: Long, questionCount: Int, difficulty: QuizDifficulty, mode: QuizMode): Long
    fun getQuiz(quizId: Long): Flow<QuizUiModel?>
    suspend fun generateQuestions(quizId: Long): QuizGenerationResult
    fun getQuizQuestions(quizId: Long): Flow<List<QuizQuestionUiModel>>
    suspend fun saveQuizResult(quizId: Long, score: Int, durationSec: Int, correctCount: Int, wrongCount: Int, mode: QuizMode)
    fun getQuizResults(quizId: Long): Flow<List<QuizResultUiModel>>
}

enum class QuizDifficulty {
    EASY, MEDIUM, HARD
}

enum class QuizMode {
    TIMED, SURVIVAL
}

data class QuizUiModel(
    val id: Long,
    val name: String,
    val watchlistName: String,
    val createdAt: Long,
    val difficulty: QuizDifficulty,
    val mode: QuizMode,
    val questionCount: Int
)

data class QuizQuestionUiModel(
    val id: Long,
    val quizId: Long,
    val movieId: Long,
    val type: QuestionType,
    val correctAnswer: String,
    val options: List<String>,
    val difficulty: QuizDifficulty
)

enum class QuestionType {
    RELEASE_YEAR, DIRECTOR, MAIN_ACTOR, RUNTIME, GENRE
}

data class QuizResultUiModel(
    val id: Long,
    val quizId: Long,
    val playedAt: Long,
    val score: Int,
    val durationSec: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val mode: QuizMode
)

data class QuizGenerationResult(
    val success: Boolean,
    val questionsGenerated: Int,
    val message: String
)
