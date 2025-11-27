package com.example.cinephile.domain.repository

import kotlinx.coroutines.flow.Flow

interface QuizRepository {
    fun getAllQuizzes(): Flow<List<QuizUiModel>>
    suspend fun createQuiz(name: String, watchlistId: Long, questionCount: Int, difficulty: QuizDifficulty = QuizDifficulty.EASY, mode: QuizMode = QuizMode.TIMED): Long
    fun getQuiz(quizId: Long): Flow<QuizUiModel?>
    suspend fun generateQuestions(quizId: Long): QuizGenerationResult
    fun getQuizQuestions(quizId: Long): Flow<List<QuizQuestionUiModel>>
    suspend fun saveQuizResult(quizId: Long, xpEarned: Int, durationSec: Int, correctCount: Int, wrongCount: Int)
    fun getQuizResults(quizId: Long): Flow<List<QuizResultUiModel>>
    suspend fun getWatchlistMovieCount(watchlistId: Long): Int
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
    val questionCount: Int,
    val difficulty: String? = null,
    val mode: String? = null
)

data class QuizQuestionUiModel(
    val id: Long,
    val quizId: Long,
    val movieId: Long,
    val type: QuestionType,
    val correctAnswer: String,
    val options: List<String>,
    val difficulty: QuizDifficulty,
    val moviePosterUrl: String? = null, 
    val movieDescription: String? = null 
)

enum class QuestionType {
    MOVIE_FROM_DESCRIPTION, 
    ACTOR_IN_MOVIE, 
    RELEASE_YEAR, 
    MOVIE_FROM_IMAGE 
}

data class QuizResultUiModel(
    val id: Long,
    val quizId: Long,
    val playedAt: Long,
    val xpEarned: Int,
    val durationSec: Int,
    val correctCount: Int,
    val wrongCount: Int
)

data class QuizGenerationResult(
    val success: Boolean,
    val questionsGenerated: Int,
    val message: String
)
