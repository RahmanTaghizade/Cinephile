package com.example.cinephile.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cinephile.data.local.dao.QuizDao
import com.example.cinephile.data.local.entities.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuizDaoTest {
    
    private lateinit var database: CinephileDb
    private lateinit var quizDao: QuizDao
    private lateinit var watchlistDao: com.example.cinephile.data.local.dao.WatchlistDao
    private lateinit var movieDao: com.example.cinephile.data.local.dao.MovieDao
    
    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CinephileDb::class.java
        ).allowMainThreadQueries().build()
        quizDao = database.quizDao()
        watchlistDao = database.watchlistDao()
        movieDao = database.movieDao()
    }
    
    @After
    fun closeDb() {
        database.close()
    }
    
    @Test
    fun insertQuizAndListQuizzes() = runBlocking {
        
        val watchlistId = watchlistDao.insert(WatchlistEntity(name = "Test Watchlist"))
        
        val quiz1 = QuizEntity(
            name = "Quiz 1",
            watchlistId = watchlistId,
            createdAt = System.currentTimeMillis() - 1000, 
            difficulty = "Easy",
            mode = "Timed",
            questionCount = 10
        )
        
        val quiz2 = QuizEntity(
            name = "Quiz 2",
            watchlistId = watchlistId,
            createdAt = System.currentTimeMillis(), 
            difficulty = "Medium",
            mode = "Survival",
            questionCount = 15
        )
        
        
        val id1 = quizDao.insertQuiz(quiz1)
        val id2 = quizDao.insertQuiz(quiz2)
        val quizzes = quizDao.listQuizzes().first()
        
        
        assertEquals(2, quizzes.size)
        
        assertEquals(id2, quizzes[0].id)
        assertEquals("Quiz 2", quizzes[0].name)
        assertEquals(id1, quizzes[1].id)
        assertEquals("Quiz 1", quizzes[1].name)
    }
    
    @Test
    fun getQuiz() = runBlocking {
        
        val watchlistId = watchlistDao.insert(WatchlistEntity(name = "Test Watchlist"))
        val quiz = QuizEntity(
            name = "Test Quiz",
            watchlistId = watchlistId,
            createdAt = System.currentTimeMillis(),
            difficulty = "Hard",
            mode = "Timed",
            questionCount = 20
        )
        
        
        val insertedId = quizDao.insertQuiz(quiz)
        val retrievedQuiz = quizDao.getQuiz(insertedId)
        
        
        assertNotNull(retrievedQuiz)
        assertEquals(insertedId, retrievedQuiz?.id)
        assertEquals("Test Quiz", retrievedQuiz?.name)
        assertEquals("Hard", retrievedQuiz?.difficulty)
        assertEquals("Timed", retrievedQuiz?.mode)
        assertEquals(20, retrievedQuiz?.questionCount)
    }
    
    @Test
    fun getQuizNotFound() = runBlocking {
        
        val retrievedQuiz = quizDao.getQuiz(999L)
        
        
        assertNull(retrievedQuiz)
    }
    
    @Test
    fun insertQuestionsAndListQuestions() = runBlocking {
        
        val watchlistId = watchlistDao.insert(WatchlistEntity(name = "Test Watchlist"))
        val quizId = quizDao.insertQuiz(QuizEntity(
            name = "Test Quiz",
            watchlistId = watchlistId,
            createdAt = System.currentTimeMillis(),
            difficulty = "Easy",
            mode = "Timed",
            questionCount = 2
        ))
        
        val movieId = 12345L
        movieDao.upsert(MovieEntity(
            id = movieId,
            title = "Test Movie",
            posterPath = null,
            overview = null,
            releaseDate = null,
            directorId = null,
            directorName = null,
            castIds = emptyList(),
            castNames = emptyList(),
            genreIds = emptyList(),
            keywordIds = emptyList(),
            runtime = null,
            lastUpdated = System.currentTimeMillis()
        ))
        
        val question1 = QuizQuestionEntity(
            quizId = quizId,
            movieId = movieId,
            type = "release_year",
            correctAnswer = "2020",
            optionsJson = """["2018","2019","2020","2021"]""",
            difficulty = "Easy"
        )
        
        val question2 = QuizQuestionEntity(
            quizId = quizId,
            movieId = movieId,
            type = "director",
            correctAnswer = "John Doe",
            optionsJson = """["John Doe","Jane Smith","Bob Johnson","Alice Brown"]""",
            difficulty = "Easy"
        )
        
        
        quizDao.insertQuestions(listOf(question1, question2))
        val questions = quizDao.listQuestions(quizId)
        
        
        assertEquals(2, questions.size)
        assertEquals("release_year", questions[0].type)
        assertEquals("2020", questions[0].correctAnswer)
        assertEquals("director", questions[1].type)
        assertEquals("John Doe", questions[1].correctAnswer)
    }
    
    @Test
    fun insertResultAndListResults() = runBlocking {
        
        val watchlistId = watchlistDao.insert(WatchlistEntity(name = "Test Watchlist"))
        val quizId = quizDao.insertQuiz(QuizEntity(
            name = "Test Quiz",
            watchlistId = watchlistId,
            createdAt = System.currentTimeMillis(),
            difficulty = "Medium",
            mode = "Timed",
            questionCount = 10
        ))
        
        val result1 = QuizResultEntity(
            quizId = quizId,
            playedAt = System.currentTimeMillis() - 2000, 
            score = 80,
            durationSec = 300,
            correctCount = 8,
            wrongCount = 2,
            mode = "Timed"
        )
        
        val result2 = QuizResultEntity(
            quizId = quizId,
            playedAt = System.currentTimeMillis(), 
            score = 90,
            durationSec = 250,
            correctCount = 9,
            wrongCount = 1,
            mode = "Timed"
        )
        
        
        val id1 = quizDao.insertResult(result1)
        val id2 = quizDao.insertResult(result2)
        val results = quizDao.listResults(quizId).first()
        
        
        assertEquals(2, results.size)
        
        assertEquals(id2, results[0].id)
        assertEquals(90, results[0].score)
        assertEquals(id1, results[1].id)
        assertEquals(80, results[1].score)
    }
    
    @Test
    fun listResultsForNonExistentQuiz() = runBlocking {
        
        val results = quizDao.listResults(999L).first()
        
        
        assertTrue(results.isEmpty())
    }
    
    @Test
    fun listQuestionsForNonExistentQuiz() = runBlocking {
        
        val questions = quizDao.listQuestions(999L)
        
        
        assertTrue(questions.isEmpty())
    }
}

