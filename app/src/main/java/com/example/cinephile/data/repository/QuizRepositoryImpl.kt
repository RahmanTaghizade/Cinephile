package com.example.cinephile.data.repository

import com.example.cinephile.data.local.dao.QuizDao
import com.example.cinephile.data.local.dao.WatchlistDao
import com.example.cinephile.data.local.entities.QuizEntity
import com.example.cinephile.data.local.entities.QuizResultEntity
import com.example.cinephile.data.local.entities.QuizQuestionEntity
import com.example.cinephile.data.local.converters.ListConverters
import com.example.cinephile.domain.repository.QuestionType
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
    private val watchlistDao: WatchlistDao,
    private val listConverters: ListConverters
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
        return quizDao.listQuizzes().map { quizzes ->
            quizzes.find { it.id == quizId }
        }.flatMapLatest { quizEntity ->
            if (quizEntity == null) {
                flowOf(null)
            } else {
                flow {
                    val watchlistName = watchlistDao.getById(quizEntity.watchlistId)?.name ?: "Unknown"
                    emit(
                        QuizUiModel(
                            id = quizEntity.id,
                            name = quizEntity.name,
                            watchlistName = watchlistName,
                            createdAt = quizEntity.createdAt,
                            difficulty = when (quizEntity.difficulty) {
                                "Easy" -> QuizDifficulty.EASY
                                "Medium" -> QuizDifficulty.MEDIUM
                                "Hard" -> QuizDifficulty.HARD
                                else -> QuizDifficulty.EASY
                            },
                            mode = when (quizEntity.mode) {
                                "Timed" -> QuizMode.TIMED
                                "Survival" -> QuizMode.SURVIVAL
                                else -> QuizMode.TIMED
                            },
                            questionCount = quizEntity.questionCount
                        )
                    )
                }
            }
        }
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
        return quizDao.observeQuestions(quizId).map { questions ->
            questions.map { question ->
                val options = listConverters.toStringList(question.optionsJson) ?: emptyList()
                QuizQuestionUiModel(
                    id = question.id,
                    quizId = question.quizId,
                    movieId = question.movieId,
                    type = when (question.type) {
                        "release_year" -> QuestionType.RELEASE_YEAR
                        "director" -> QuestionType.DIRECTOR
                        "main_actor" -> QuestionType.MAIN_ACTOR
                        "runtime" -> QuestionType.RUNTIME
                        "genre" -> QuestionType.GENRE
                        else -> QuestionType.RELEASE_YEAR
                    },
                    correctAnswer = question.correctAnswer,
                    options = options,
                    difficulty = when (question.difficulty) {
                        "Easy" -> QuizDifficulty.EASY
                        "Medium" -> QuizDifficulty.MEDIUM
                        "Hard" -> QuizDifficulty.HARD
                        else -> QuizDifficulty.EASY
                    }
                )
            }
        }
    }

    override suspend fun saveQuizResult(quizId: Long, score: Int, durationSec: Int, correctCount: Int, wrongCount: Int, mode: QuizMode) {
        val resultEntity = com.example.cinephile.data.local.entities.QuizResultEntity(
            quizId = quizId,
            playedAt = System.currentTimeMillis(),
            score = score,
            durationSec = durationSec,
            correctCount = correctCount,
            wrongCount = wrongCount,
            mode = when (mode) {
                QuizMode.TIMED -> "Timed"
                QuizMode.SURVIVAL -> "Survival"
            }
        )
        quizDao.insertResult(resultEntity)
    }

    override fun getQuizResults(quizId: Long): Flow<List<QuizResultUiModel>> {
        return quizDao.listResults(quizId).map { results ->
            results.map { result ->
                QuizResultUiModel(
                    id = result.id,
                    quizId = result.quizId,
                    playedAt = result.playedAt,
                    score = result.score,
                    durationSec = result.durationSec,
                    correctCount = result.correctCount,
                    wrongCount = result.wrongCount,
                    mode = when (result.mode) {
                        "Timed" -> QuizMode.TIMED
                        "Survival" -> QuizMode.SURVIVAL
                        else -> QuizMode.TIMED
                    }
                )
            }
        }
    }
}
