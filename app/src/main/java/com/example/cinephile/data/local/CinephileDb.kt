package com.example.cinephile.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.cinephile.data.local.converters.ListConverters
import com.example.cinephile.data.local.dao.*
import com.example.cinephile.data.local.entities.*

@Database(
    entities = [
        MovieEntity::class,
        WatchlistEntity::class,
        WatchlistMovieCrossRef::class,
        QuizEntity::class,
        QuizQuestionEntity::class,
        QuizResultEntity::class,
        CachedSearchEntity::class,
        GenreEntity::class,
        RecommendedMovieEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(ListConverters::class)
abstract class CinephileDb : RoomDatabase() {
    
    abstract fun movieDao(): MovieDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun quizDao(): QuizDao
    abstract fun cachedSearchDao(): CachedSearchDao
    abstract fun genreDao(): GenreDao
    abstract fun recommendationDao(): RecommendationDao
    
    companion object {
        const val DATABASE_NAME = "cinephile_database"
        
        fun create(context: Context): CinephileDb {
            return Room.databaseBuilder(
                context.applicationContext,
                CinephileDb::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
