package com.example.cinephile.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val watchlistId: Long,
    val createdAt: Long,
    val difficulty: String, 
    val mode: String, 
    val questionCount: Int
)
