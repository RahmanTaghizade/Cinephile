package com.example.cinephile.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cinephile.data.local.dao.WatchlistDao
import com.example.cinephile.data.local.entities.WatchlistEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WatchlistDaoTest {
    
    private lateinit var database: CinephileDb
    private lateinit var watchlistDao: WatchlistDao
    
    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CinephileDb::class.java
        ).allowMainThreadQueries().build()
        watchlistDao = database.watchlistDao()
    }
    
    @After
    fun closeDb() {
        database.close()
    }
    
    @Test
    fun insertAndGetWatchlist() = runBlocking {
        // Given
        val watchlist = WatchlistEntity(
            name = "Test Watchlist",
            isCurrent = true
        )
        
        // When
        val insertedId = watchlistDao.insert(watchlist)
        val retrievedWatchlist = watchlistDao.getCurrent()
        
        // Then
        assertNotNull(retrievedWatchlist)
        assertEquals("Test Watchlist", retrievedWatchlist?.name)
        assertTrue(retrievedWatchlist?.isCurrent ?: false)
        assertEquals(insertedId, retrievedWatchlist?.id)
    }
    
    @Test
    fun setCurrentWatchlist() = runBlocking {
        // Given
        val watchlist1 = WatchlistEntity(name = "Watchlist 1", isCurrent = false)
        val watchlist2 = WatchlistEntity(name = "Watchlist 2", isCurrent = false)
        
        val id1 = watchlistDao.insert(watchlist1)
        val id2 = watchlistDao.insert(watchlist2)
        
        // When
        watchlistDao.setCurrentWatchlist(id2)
        
        // Then
        val current = watchlistDao.getCurrent()
        assertNotNull(current)
        assertEquals(id2, current?.id)
        assertEquals("Watchlist 2", current?.name)
        assertTrue(current?.isCurrent ?: false)
    }
}
