package com.example.cinephile.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cinephile.R
import com.example.cinephile.databinding.FragmentSearchBinding
import com.example.cinephile.ui.search.SearchFragmentDirections
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import com.example.cinephile.util.ConnectivityMonitor

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private val args: SearchFragmentArgs by navArgs()
    private val viewModel: SearchViewModel by viewModels()
    @Inject lateinit var connectivityMonitor: ConnectivityMonitor
    private lateinit var resultsAdapter: SearchResultsAdapter

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
        observeConnectivity()
        setupFilterChips()
        setupSearchInput()
        observeUiEvents()
        setupGenreChip()
        setupFilterButton()
        handleInitialArguments(savedInstanceState)
    }
    
    private fun setupFilterButton() {
        binding.buttonFilter.setOnClickListener {
            showFilterBottomSheet()
        }
    }
    
    private fun showFilterBottomSheet() {
        val filterBottomSheet = FilterBottomSheetDialogFragment()
        filterBottomSheet.show(childFragmentManager, FilterBottomSheetDialogFragment.TAG)
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

    private fun setupGenreChip() {
        binding.chipActiveGenre.setOnCloseIconClickListener {
            viewModel.clearActiveGenre()
        }
        binding.chipActiveGenre.setOnClickListener {
            viewModel.clearActiveGenre()
        }
    }

    private fun handleInitialArguments(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) return

        var shouldTriggerSearch = false
        args.initialQuery?.takeIf { it.isNotBlank() }?.let { query ->
            binding.editTextTitle.setText(query)
            binding.editTextTitle.setSelection(query.length)
            viewModel.onQueryChanged(query)
            shouldTriggerSearch = true
        }

        if (args.initialGenreId != NO_GENRE_SENTINEL) {
            viewModel.applyGenreFromHome(args.initialGenreId, args.initialGenreName)
            shouldTriggerSearch = false
        }

        if (shouldTriggerSearch) {
            viewModel.onSearchClick()
        }
    }

    private fun setupRecyclerView() {
        resultsAdapter = SearchResultsAdapter(
            onMovieClick = { movieId ->
                val action = SearchFragmentDirections.actionSearchFragmentToDetailsFragment(movieId)
                findNavController().navigate(action)
            },
            onPersonClick = { person ->
                val action = SearchFragmentDirections.actionSearchFragmentToActorProfileFragment(
                    actorId = person.id,
                    actorName = person.name
                )
                findNavController().navigate(action)
            },
            onSeriesClick = {
                Snackbar.make(requireView(), getString(R.string.series_details_not_available), Snackbar.LENGTH_SHORT).show()
            }
        )

        binding.recyclerResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = resultsAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.searchUiState.collect { state ->
                // Update combined results
                resultsAdapter.submitList(state.results)

                // Handle loading state
                binding.progressLoading.visibility = if (state.isLoading && state.results.isEmpty()) View.VISIBLE else View.GONE
                
                // Handle empty state
                val isEmpty = !state.isLoading && state.results.isEmpty() && state.error == null
                binding.textEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.recyclerResults.visibility = if (isEmpty || state.isLoading) View.GONE else View.VISIBLE
                
                // Handle error state
                val hasError = state.error != null && !state.isLoading
                binding.errorCard.visibility = if (hasError) View.VISIBLE else View.GONE
                if (hasError) {
                    binding.textError.text = state.error
                    binding.recyclerResults.visibility = View.GONE
                }
                
                // Show recycler view when we have data
                if (!state.isLoading && state.results.isNotEmpty() && state.error == null) {
                    binding.recyclerResults.visibility = View.VISIBLE
                }

                // When results are from cache, show banner with timestamp text
                if (state.isOffline && state.cacheTimestamp != null) {
                    val timestamp = java.text.SimpleDateFormat(
                        "MMM dd, yyyy HH:mm",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date(state.cacheTimestamp))
                    binding.offlineText.text = getString(R.string.results_from_cache_with_timestamp, timestamp)
                    binding.offlineBanner.visibility = View.VISIBLE
                } else if (!state.isOffline) {
                    binding.offlineBanner.visibility = View.GONE
                }

                binding.chipActiveGenre.isVisible = state.activeGenreName != null
                binding.chipActiveGenre.text = state.activeGenreName ?: ""
            }
        }
        
        // Setup retry button
        binding.buttonRetry.setOnClickListener {
            viewModel.onSearchClick()
        }
    }

    private fun observeConnectivity() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            connectivityMonitor.isOnlineFlow.collect { isOnline ->
                if (!isOnline) {
                    // Show generic offline banner text when offline and no cached timestamp yet
                    binding.offlineText.text = getString(R.string.results_from_cache)
                    binding.offlineBanner.visibility = View.VISIBLE
                } else {
                    // Only hide if not currently showing cached timestamp state
                    if (!viewModel.searchUiState.value.isOffline) {
                        binding.offlineBanner.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun observeUiEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is SearchUiEvent.ShowAddedToWatchlist -> {
                        Snackbar.make(requireView(), getString(R.string.added_to_watchlist), Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo) { viewModel.undoAdd() }
                            .show()
                    }
                    is SearchUiEvent.ShowUndoFailed -> {
                        Snackbar.make(requireView(), getString(R.string.action_failed), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupFilterChips() {
        val chipGroup = binding.chipGroupFilters
        chipGroup.removeAllViews()
        val items = listOf(
            "All" to SearchFilter.ALL,
            "Movies" to SearchFilter.MOVIES,
            "Series" to SearchFilter.SERIES,
            "Actors" to SearchFilter.ACTORS,
            "Producers" to SearchFilter.PRODUCERS
        )
        items.forEachIndexed { index, pair ->
            val chip = Chip(requireContext())
            chip.text = pair.first
            chip.isCheckable = true
            chip.isChecked = index == 0
            chip.setOnClickListener { viewModel.onFilterSelected(pair.second) }
            chipGroup.addView(chip)
        }
    }

    // Removed actor/director suggestion chips in favor of unified filter chips

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val NO_GENRE_SENTINEL = -1
    }
}
