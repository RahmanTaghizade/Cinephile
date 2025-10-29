package com.example.cinephile.ui.search

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cinephile.R
import com.example.cinephile.databinding.FragmentSearchBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var movieAdapter: MovieAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
        setupSearchInput()
    }
    
    private fun setupSearchInput() {
        // Handle text changes in the search input
        binding.editTextTitle.setOnEditorActionListener { _, _, _ ->
            viewModel.onSearchClick()
            true
        }
        
        binding.editTextTitle.doAfterTextChanged { text ->
            viewModel.onQueryChanged(text.toString())
        }
        
        // Handle search button click
        binding.buttonSearch.setOnClickListener {
            viewModel.onSearchClick()
        }
    }

    private fun setupRecyclerView() {
        val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            resources.getInteger(R.integer.movie_grid_span_portrait)
        } else {
            resources.getInteger(R.integer.movie_grid_span_landscape)
        }

        movieAdapter = MovieAdapter(
            onItemClick = { movieId ->
                // TODO: Navigate to DetailsFragment
            },
            onLongPress = { movieId ->
                // TODO: Show snackbar for adding to watchlist
            }
        )

        binding.recyclerViewMovies.apply {
            layoutManager = GridLayoutManager(context, spanCount)
            adapter = movieAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.searchUiState.collect { state ->
                // Update movie list
                movieAdapter.submitList(state.movies)
                
                // Handle loading state (can add progress bar later)
                if (state.isLoading) {
                    // Show loading indicator if needed
                }
                
                // Handle error state
                state.error?.let { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
