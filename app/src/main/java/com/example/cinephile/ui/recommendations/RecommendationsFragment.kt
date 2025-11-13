package com.example.cinephile.ui.recommendations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cinephile.databinding.FragmentRecommendationsBinding
import com.example.cinephile.ui.search.MovieAdapter
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecommendationsFragment : Fragment() {
    private var _binding: FragmentRecommendationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecommendationsViewModel by viewModels()

    private lateinit var recommendationsAdapter: MovieAdapter
    private lateinit var latestAdapter: HomeMovieCarouselAdapter
    private lateinit var upcomingAdapter: HomeMovieCarouselAdapter

    private var renderedGenres: List<RecommendationsViewModel.GenreChipUiModel> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupListeners()
        observeUiState()
    }

    private fun setupRecyclerViews() {
        latestAdapter = HomeMovieCarouselAdapter(::navigateToDetails)
        binding.recyclerLatest.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            adapter = latestAdapter
            setHasFixedSize(true)
        }

        upcomingAdapter = HomeMovieCarouselAdapter(::navigateToDetails)
        binding.recyclerUpcoming.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            adapter = upcomingAdapter
            setHasFixedSize(true)
        }

        recommendationsAdapter = MovieAdapter(onItemClick = ::navigateToDetails)
        binding.recyclerRecommendations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recommendationsAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupListeners() {
        binding.buttonRefresh.setOnClickListener { viewModel.refreshRecommendations() }
        binding.cardSearchShortcut.setOnClickListener { navigateToSearch() }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    val hasAnyContent = state.recommendations.isNotEmpty() ||
                        state.latestMovies.isNotEmpty() ||
                        state.upcomingMovies.isNotEmpty()

                    binding.progressBar.isVisible = state.isLoading && !hasAnyContent
                    binding.homeScroll.isVisible = hasAnyContent || state.isLoading
                    binding.chipCached.isVisible = state.isCached

                    binding.textEmpty.isVisible = !state.isLoading && !hasAnyContent && state.errorMessage.isNullOrEmpty()
                    binding.textError.isVisible = !state.isLoading && state.errorMessage != null && !hasAnyContent
                    binding.textError.text = state.errorMessage

                    binding.textLatestHeader.isVisible = state.latestMovies.isNotEmpty()
                    binding.recyclerLatest.isVisible = state.latestMovies.isNotEmpty()
                    latestAdapter.submitList(state.latestMovies)

                    binding.textUpcomingHeader.isVisible = state.upcomingMovies.isNotEmpty()
                    binding.recyclerUpcoming.isVisible = state.upcomingMovies.isNotEmpty()
                    upcomingAdapter.submitList(state.upcomingMovies)

                    binding.textRecommendationsHeader.isVisible = state.recommendations.isNotEmpty()
                    binding.recyclerRecommendations.isVisible = state.recommendations.isNotEmpty()
                    recommendationsAdapter.submitList(state.recommendations)

                    renderGenreChips(state.genres)
                }
            }
        }
    }

    private fun renderGenreChips(genres: List<RecommendationsViewModel.GenreChipUiModel>) {
        if (genres == renderedGenres) return
        renderedGenres = genres

        val chipGroup = binding.chipGroupGenres
        chipGroup.removeAllViews()

        if (genres.isEmpty()) {
            chipGroup.isVisible = false
            return
        }

        chipGroup.isVisible = true
        genres.forEach { genre ->
            val chip = Chip(requireContext()).apply {
                text = genre.name
                isCheckable = false
                isClickable = true
                setOnClickListener { navigateToSearch(genre.id, genre.name) }
            }
            chipGroup.addView(chip)
        }
    }

    private fun navigateToDetails(movieId: Long) {
        val action = RecommendationsFragmentDirections.actionHomeFragmentToDetailsFragment(movieId)
        findNavController().navigate(action)
    }

    private fun navigateToSearch(
        initialGenreId: Int? = null,
        initialGenreName: String? = null,
        initialQuery: String? = null
    ) {
        val genreIdArg = initialGenreId ?: NO_GENRE_SENTINEL
        val action = RecommendationsFragmentDirections
            .actionHomeFragmentToSearchFragment(genreIdArg, initialGenreName, initialQuery)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val NO_GENRE_SENTINEL = -1
    }
}
