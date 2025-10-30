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
import com.google.android.material.chip.Chip
import android.widget.AutoCompleteTextView
import com.example.cinephile.data.remote.TmdbPerson

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
        observeGenreChips()
        setupPersonSuggestions()
        observePersonChips()
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
            
            // Add scroll listener for endless scrolling
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                    val visibleThreshold = 5 // Load more when 5 items from the end
                    
                    if (totalItemCount > 0 && lastVisibleItem + visibleThreshold >= totalItemCount) {
                        viewModel.loadNextPage()
                    }
                }
            })
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
                
                // Handle offline banner
                binding.offlineBanner.visibility = if (state.isOffline && state.cacheTimestamp != null) {
                    val timestamp = java.text.SimpleDateFormat(
                        "MMM dd, yyyy HH:mm",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date(state.cacheTimestamp))
                    binding.offlineText.text = getString(R.string.results_from_cache_with_timestamp, timestamp)
                    View.VISIBLE
                } else {
                    View.GONE
                }
                
                // Handle error state
                state.error?.let { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun observeGenreChips() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.genres.collect { genres ->
                val chipGroup = binding.chipGroupGenres
                chipGroup.removeAllViews()
                genres.forEach { genre ->
                    val chip = Chip(requireContext())
                    chip.text = genre.name
                    chip.isCheckable = true
                    chip.isChecked = viewModel.selectedGenreIds.value.contains(genre.id)
                    chip.setOnClickListener {
                        viewModel.onGenreChipClicked(genre.id)
                    }
                    chipGroup.addView(chip)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedGenreIds.collect { selected ->
                val chipGroup = binding.chipGroupGenres
                for (i in 0 until chipGroup.childCount) {
                    val chip = chipGroup.getChildAt(i) as? Chip ?: continue
                    val genre = viewModel.genres.value.getOrNull(i)
                    chip.isChecked = genre?.let { selected.contains(it.id) } == true
                }
            }
        }
    }

    private lateinit var actorAdapter: PersonSearchAdapter
    private lateinit var directorAdapter: PersonSearchAdapter

    private fun setupPersonSuggestions() {
        actorAdapter = PersonSearchAdapter(requireContext())
        directorAdapter = PersonSearchAdapter(requireContext())
        binding.autocompleteActor.setAdapter(actorAdapter)
        binding.autocompleteDirector.setAdapter(directorAdapter)
        binding.autocompleteActor.threshold = 1
        binding.autocompleteDirector.threshold = 1

        binding.autocompleteActor.doAfterTextChanged { text ->
            viewModel.onActorSearchQueryChanged(text?.toString().orEmpty())
        }
        binding.autocompleteDirector.doAfterTextChanged { text ->
            viewModel.onDirectorSearchQueryChanged(text?.toString().orEmpty())
        }
        binding.autocompleteActor.setOnItemClickListener { _, _, position, _ ->
            val person = actorAdapter.getItem(position)
            person?.let {
                viewModel.addSelectedActor(it)
                binding.autocompleteActor.setText("")
            }
        }
        binding.autocompleteDirector.setOnItemClickListener { _, _, position, _ ->
            val person = directorAdapter.getItem(position)
            person?.let {
                viewModel.addSelectedDirector(it)
                binding.autocompleteDirector.setText("")
            }
        }

        lifecycleScope.launch {
            viewModel.actorSuggestions.collect { suggestions ->
                actorAdapter.updateData(suggestions)
            }
        }
        lifecycleScope.launch {
            viewModel.directorSuggestions.collect { suggestions ->
                directorAdapter.updateData(suggestions)
            }
        }
    }

    private fun observePersonChips() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedActors.collect { actors ->
                val chipGroup = binding.chipGroupActors
                chipGroup.removeAllViews()
                actors.forEach { actor ->
                    val chip = Chip(requireContext())
                    chip.text = actor.name
                    chip.isCloseIconVisible = true
                    chip.setOnCloseIconClickListener { viewModel.removeSelectedActor(actor.id) }
                    chipGroup.addView(chip)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedDirectors.collect { directors ->
                val chipGroup = binding.chipGroupDirectors
                chipGroup.removeAllViews()
                directors.forEach { director ->
                    val chip = Chip(requireContext())
                    chip.text = director.name
                    chip.isCloseIconVisible = true
                    chip.setOnCloseIconClickListener { viewModel.removeSelectedDirector(director.id) }
                    chipGroup.addView(chip)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
