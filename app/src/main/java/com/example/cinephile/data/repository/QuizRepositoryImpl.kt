package com.example.cinephile.data.repository

import com.example.cinephile.data.local.dao.QuizDao
import com.example.cinephile.data.local.dao.WatchlistDao
import com.example.cinephile.data.local.dao.MovieDao
import com.example.cinephile.data.local.entities.QuizEntity
import com.example.cinephile.data.local.entities.QuizResultEntity
import com.example.cinephile.data.local.entities.QuizQuestionEntity
import com.example.cinephile.data.local.entities.MovieEntity
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
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val quizDao: QuizDao,
    private val watchlistDao: WatchlistDao,
    private val movieDao: MovieDao,
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
                        questionCount = quiz.questionCount,
                        difficulty = quiz.difficulty,
                        mode = quiz.mode
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
            difficulty = difficulty.name, 
            mode = mode.name, 
            questionCount = questionCount
        )
        return quizDao.insertQuiz(quizEntity)
    }
    
    override suspend fun getWatchlistMovieCount(watchlistId: Long): Int {
        return watchlistDao.getMovieCount(watchlistId)
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
                            questionCount = quizEntity.questionCount,
                            difficulty = quizEntity.difficulty,
                            mode = quizEntity.mode
                        )
                    )
                }
            }
        }
    }

    override suspend fun generateQuestions(quizId: Long): QuizGenerationResult {
        val quiz = quizDao.getQuiz(quizId) ?: return QuizGenerationResult(
            success = false,
            questionsGenerated = 0,
            message = "Quiz not found"
        )
        
        
        val movieIds = watchlistDao.getMovieIds(quiz.watchlistId)
        if (movieIds.size < 4) {
            return QuizGenerationResult(
                success = false,
                questionsGenerated = 0,
                message = "Watchlist must have at least 4 movies"
            )
        }
        
        val movies = movieDao.getByIds(movieIds)
        if (movies.size < 4) {
            return QuizGenerationResult(
                success = false,
                questionsGenerated = 0,
                message = "Not enough movie data available"
            )
        }
        
        
        quizDao.deleteQuestions(quizId)
        
        val questions = mutableListOf<QuizQuestionEntity>()
        val questionTypes = listOf(
            QuestionType.MOVIE_FROM_DESCRIPTION,
            QuestionType.ACTOR_IN_MOVIE,
            QuestionType.RELEASE_YEAR,
            QuestionType.MOVIE_FROM_IMAGE
        )
        
        val difficulty = try {
            QuizDifficulty.valueOf(quiz.difficulty ?: "EASY")
        } catch (e: Exception) {
            QuizDifficulty.EASY
        }
        
        repeat(quiz.questionCount) { index ->
            val questionType = questionTypes[index % questionTypes.size]
            val selectedMovie = selectMovieByDifficulty(movies, difficulty)
            val otherMovies = movies.filter { it.id != selectedMovie.id }.shuffled()
            
            when (questionType) {
                QuestionType.MOVIE_FROM_DESCRIPTION -> {
                    val optionCount = getOptionCountByDifficulty(difficulty)
                    val options = (listOf(selectedMovie) + otherMovies.take(optionCount - 1)).shuffled()
                    val posterUrl = selectedMovie.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                    val description = if (difficulty == QuizDifficulty.HARD) {
                        selectedMovie.overview?.take(100) ?: ""
                    } else {
                        selectedMovie.overview
                    }
                    questions.add(
                        QuizQuestionEntity(
                            quizId = quizId,
                            movieId = selectedMovie.id,
                            type = "movie_from_description",
                            correctAnswer = selectedMovie.title,
                            optionsJson = listConverters.fromStringList(options.map { it.title }) ?: "[]",
                            difficulty = difficulty.name,
                            moviePosterUrl = posterUrl,
                            movieDescription = description
                        )
                    )
                }
                QuestionType.ACTOR_IN_MOVIE -> {
                    if (selectedMovie.castNames.isNotEmpty()) {
                        val actorIndex = getActorIndexByDifficulty(difficulty, selectedMovie.castNames.size)
                        val correctActor = selectedMovie.castNames.getOrNull(actorIndex) ?: selectedMovie.castNames.first()
                        val otherActors = movies.flatMap { it.castNames }
                            .filter { it != correctActor }
                            .distinct()
                            .shuffled()
                            .take(getOptionCountByDifficulty(difficulty) - 1)
                        val options = (listOf(correctActor) + otherActors).shuffled()
                        questions.add(
                            QuizQuestionEntity(
                                quizId = quizId,
                                movieId = selectedMovie.id,
                                type = "actor_in_movie",
                                correctAnswer = correctActor,
                                optionsJson = listConverters.fromStringList(options) ?: "[]",
                                difficulty = difficulty.name,
                                moviePosterUrl = selectedMovie.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                            )
                        )
                    }
                }
                QuestionType.RELEASE_YEAR -> {
                    val releaseYear = selectedMovie.releaseDate?.take(4) ?: "Unknown"
                    val yearRange = getYearRangeByDifficulty(difficulty)
                    val correctYearInt = releaseYear.toIntOrNull() ?: 2000
                    val otherYears = generateYearOptions(correctYearInt, yearRange, movies)
                        .shuffled()
                        .take(getOptionCountByDifficulty(difficulty) - 1)
                    val options = (listOf(releaseYear) + otherYears).shuffled()
                    questions.add(
                        QuizQuestionEntity(
                            quizId = quizId,
                            movieId = selectedMovie.id,
                            type = "release_year",
                            correctAnswer = releaseYear,
                            optionsJson = listConverters.fromStringList(options) ?: "[]",
                            difficulty = difficulty.name,
                            moviePosterUrl = selectedMovie.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                        )
                    )
                }
                QuestionType.MOVIE_FROM_IMAGE -> {
                    val optionCount = getOptionCountByDifficulty(difficulty)
                    val options = (listOf(selectedMovie) + otherMovies.take(optionCount - 1)).shuffled()
                    val posterUrl = selectedMovie.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                    questions.add(
                        QuizQuestionEntity(
                            quizId = quizId,
                            movieId = selectedMovie.id,
                            type = "movie_from_image",
                            correctAnswer = selectedMovie.title,
                            optionsJson = listConverters.fromStringList(options.map { it.title }) ?: "[]",
                            difficulty = difficulty.name,
                            moviePosterUrl = posterUrl
                        )
                    )
                }
            }
        }
        
        quizDao.insertQuestions(questions)
        
        return QuizGenerationResult(
            success = true,
            questionsGenerated = questions.size,
            message = "Questions generated successfully"
        )
    }

    private fun selectMovieByDifficulty(movies: List<MovieEntity>, difficulty: QuizDifficulty): MovieEntity {
        return when (difficulty) {
            QuizDifficulty.EASY -> {
                movies.filter { it.overview != null && it.overview.isNotBlank() && it.castNames.isNotEmpty() }.randomOrNull() ?: movies.random()
            }
            QuizDifficulty.MEDIUM -> {
                val mediumMovies = movies.filter { 
                    it.overview != null && it.overview.length > 100 && it.castNames.size >= 2 
                }
                if (mediumMovies.isNotEmpty()) mediumMovies.random() else movies.random()
            }
            QuizDifficulty.HARD -> {
                val hardMovies = movies.filter { 
                    it.overview.isNullOrBlank() || it.castNames.isEmpty() || it.releaseDate.isNullOrBlank()
                }
                if (hardMovies.isNotEmpty()) hardMovies.random() else movies.random()
            }
        }
    }

    private fun getOptionCountByDifficulty(difficulty: QuizDifficulty): Int {
        return when (difficulty) {
            QuizDifficulty.EASY -> 4
            QuizDifficulty.MEDIUM -> 5
            QuizDifficulty.HARD -> 6
        }
    }

    private fun getActorIndexByDifficulty(difficulty: QuizDifficulty, castSize: Int): Int {
        return when (difficulty) {
            QuizDifficulty.EASY -> 0
            QuizDifficulty.MEDIUM -> minOf(1, castSize - 1)
            QuizDifficulty.HARD -> minOf(2, castSize - 1)
        }
    }

    private fun getYearRangeByDifficulty(difficulty: QuizDifficulty): Int {
        return when (difficulty) {
            QuizDifficulty.EASY -> 20
            QuizDifficulty.MEDIUM -> 10
            QuizDifficulty.HARD -> 5
        }
    }

    private fun generateYearOptions(correctYear: Int, range: Int, movies: List<MovieEntity>): List<String> {
        val allYears = movies.mapNotNull { it.releaseDate?.take(4)?.toIntOrNull() }.distinct()
        val closeYears = allYears.filter { 
            kotlin.math.abs(it - correctYear) <= range && it != correctYear 
        }
        val farYears = allYears.filter { 
            kotlin.math.abs(it - correctYear) > range 
        }
        
        val options = mutableListOf<String>()
        if (closeYears.isNotEmpty()) {
            options.addAll(closeYears.take(2).map { it.toString() })
        }
        if (farYears.isNotEmpty() && options.size < 3) {
            options.addAll(farYears.take(3 - options.size).map { it.toString() })
        }
        return options
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
                        "movie_from_description" -> QuestionType.MOVIE_FROM_DESCRIPTION
                        "actor_in_movie" -> QuestionType.ACTOR_IN_MOVIE
                        "release_year" -> QuestionType.RELEASE_YEAR
                        "movie_from_image" -> QuestionType.MOVIE_FROM_IMAGE
                        else -> QuestionType.RELEASE_YEAR
                    },
                    correctAnswer = question.correctAnswer,
                    options = options,
                    difficulty = try {
                        QuizDifficulty.valueOf(question.difficulty)
                    } catch (e: Exception) {
                        QuizDifficulty.EASY
                    },
                    moviePosterUrl = question.moviePosterUrl,
                    movieDescription = question.movieDescription
                )
            }
        }
    }

    override suspend fun saveQuizResult(quizId: Long, xpEarned: Int, durationSec: Int, correctCount: Int, wrongCount: Int) {
        try {
            // Verify quiz exists before saving result
            val quiz = quizDao.getQuiz(quizId)
            if (quiz == null) {
                android.util.Log.e("QuizRepository", "Cannot save quiz result: Quiz $quizId does not exist")
                return
            }
            
            val resultEntity = QuizResultEntity(
                quizId = quizId,
                playedAt = System.currentTimeMillis(),
                xpEarned = xpEarned,
                durationSec = durationSec,
                correctCount = correctCount,
                wrongCount = wrongCount,
                score = correctCount // Calculate score as correct count
            )
            val resultId = quizDao.insertResult(resultEntity)
            android.util.Log.d("QuizRepository", "Saved quiz result: id=$resultId, quizId=$quizId, correct=$correctCount, wrong=$wrongCount, xp=$xpEarned, duration=$durationSec")
        } catch (e: Exception) {
            android.util.Log.e("QuizRepository", "Error saving quiz result for quizId=$quizId", e)
            throw e
        }
    }

    override fun getQuizResults(quizId: Long): Flow<List<QuizResultUiModel>> {
        return quizDao.listResults(quizId).map { results ->
            android.util.Log.d("QuizRepository", "Loading quiz results for quizId=$quizId, found ${results.size} results")
            results.map { result ->
                QuizResultUiModel(
                    id = result.id,
                    quizId = result.quizId,
                    playedAt = result.playedAt,
                    xpEarned = result.xpEarned,
                    durationSec = result.durationSec,
                    correctCount = result.correctCount,
                    wrongCount = result.wrongCount
                )
            }
        }
    }
}
