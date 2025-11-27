package com.example.cinephile.domain.repository

import org.junit.Test
import org.junit.Assert.*
import com.example.cinephile.ui.search.MovieUiModel




class RepositoryInterfacesTest {

    @Test
    fun movieRepositoryInterface_hasRequiredMethods() {
        
        
        
        
        assertNotNull("MovieRepository interface should exist", MovieRepository::class.java)
        
        
        val methods = MovieRepository::class.java.declaredMethods.map { it.name }
        assertTrue("Should have searchMovies method", methods.contains("searchMovies"))
        assertTrue("Should have getMovieDetails method", methods.contains("getMovieDetails"))
        assertTrue("Should have computeContentVector method", methods.contains("computeContentVector"))
    }

    @Test
    fun movieUiModel_hasRequiredProperties() {
        
        val movie = MovieUiModel(
            id = 1L,
            title = "Test Movie",
            posterUrl = "https://example.com/poster.jpg",
            director = "Test Director",
            releaseDate = "2023-01-01",
            isFavorite = true,
            userRating = 4.5f
        )
        
        assertEquals(1L, movie.id)
        assertEquals("Test Movie", movie.title)
        assertEquals("https://example.com/poster.jpg", movie.posterUrl)
        assertEquals("Test Director", movie.director)
        assertEquals("2023-01-01", movie.releaseDate)
        assertTrue(movie.isFavorite)
        assertEquals(4.5f, movie.userRating)
    }
}
