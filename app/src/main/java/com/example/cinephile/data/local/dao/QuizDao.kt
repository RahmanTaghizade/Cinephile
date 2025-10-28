package com.example.cinephile.data.local.dao

import androidx.room.*
import com.example.cinephile.data.local.entities.QuizEntity
import com.example.cinephile.data.local.entities.QuizQuestionEntity
import com.example.cinephile.data.local.entities.QuizResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    
    @Insert
    suspend fun insertQuiz(quiz: QuizEntity): Long
    
    @Query("SELECT * FROM quizzes ORDER BY createdAt DESC")
    fun listQuizzes(): Flow<List<QuizEntity>>
    
    @Query("SELECT * FROM quizzes WHERE id = :id")
    suspend fun getQuiz(id: Long): QuizEntity?
    
    @Query("SELECT * FROM quizzes WHERE watchlistId = :watchlistId ORDER BY createdAt DESC")
    fun listQuizzesByWatchlist(watchlistId: Long): Flow<List<QuizEntity>>
    
    @Insert
    suspend fun insertQuestions(questions: List<QuizQuestionEntity>)
    
    @Query("SELECT * FROM quiz_questions WHERE quizId = :quizId ORDER BY id")
    suspend fun listQuestions(quizId: Long): List<QuizQuestionEntity>
    
    @Query("SELECT * FROM quiz_questions WHERE quizId = :quizId ORDER BY id")
    fun observeQuestions(quizId: Long): Flow<List<QuizQuestionEntity>>
    
    @Insert
    suspend fun insertResult(result: QuizResultEntity): Long
    
    @Query("SELECT * FROM quiz_results WHERE quizId = :quizId ORDER BY playedAt DESC")
    fun listResults(quizId: Long): Flow<List<QuizResultEntity>>
    
    @Query("SELECT * FROM quiz_results WHERE quizId = :quizId ORDER BY score DESC LIMIT 1")
    suspend fun getBestResult(quizId: Long): QuizResultEntity?
    
    @Query("SELECT AVG(score) FROM quiz_results WHERE quizId = :quizId")
    suspend fun getAverageScore(quizId: Long): Double?
    
    @Query("SELECT COUNT(*) FROM quiz_results WHERE quizId = :quizId")
    suspend fun getPlayCount(quizId: Long): Int
    
    @Query("DELETE FROM quizzes WHERE id = :quizId")
    suspend fun deleteQuiz(quizId: Long)
    
    @Query("DELETE FROM quiz_questions WHERE quizId = :quizId")
    suspend fun deleteQuestions(quizId: Long)
    
    @Query("DELETE FROM quiz_results WHERE quizId = :quizId")
    suspend fun deleteResults(quizId: Long)
}
