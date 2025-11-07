package com.example.cinephile.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.cinephile.databinding.FragmentDetailsBinding
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetailsFragment : Fragment() {
    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!
    private val args: DetailsFragmentArgs by navArgs()
    private val viewModel: DetailsViewModel by viewModels()
    private val castAdapter = CastAdapter()
    private var isOverviewExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup cast RecyclerView
        binding.recyclerCast.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerCast.adapter = castAdapter
        
        // Setup "more" link click listener
        binding.textMore.setOnClickListener {
            isOverviewExpanded = !isOverviewExpanded
            updateOverviewText()
        }
        
        // Observe UI state
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                val movie = state.movie
                
                binding.toolbar.title = ""
                binding.textTitle.text = movie?.title ?: ""
                
                // Format metadata: "2021, Denis Villeneuve"
                val year = movie?.releaseDate?.take(4) ?: ""
                val director = movie?.director ?: ""
                val metadata = when {
                    year.isNotEmpty() && director.isNotEmpty() -> "$year, $director"
                    year.isNotEmpty() -> year
                    director.isNotEmpty() -> director
                    else -> ""
                }
                binding.textMetadata.text = metadata
                
                // Format rating with star: "8.2 ⭐"
                val rating = movie?.voteAverage ?: 0.0
                binding.textRating.text = if (rating > 0) {
                    "%.1f ⭐".format(rating)
                } else {
                    ""
                }
                
                // Update overview
                updateOverviewText()
                
                // Update genres
                updateGenres(movie?.genres ?: emptyList())
                
                // Update cast
                castAdapter.submitList(movie?.cast ?: emptyList())
                
                // Update poster
                binding.imagePoster.load(state.posterUrl) {
                    crossfade(true)
                }
                
                // Update watchlist button
                binding.buttonWatchlist.text = "Watch now"
                
                // Show Snackbar if there's a message
                state.snackbarMessage?.let { message ->
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    viewModel.clearSnackbarMessage()
                }
            }
        }

        // Toolbar back navigation
        binding.toolbar.setNavigationOnClickListener {
            if (!findNavController().navigateUp()) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.buttonWatchlist.setOnClickListener { viewModel.toggleWatchlist() }
    }
    
    private fun updateOverviewText() {
        val overview = viewModel.uiState.value.movie?.overview ?: ""
        if (overview.isEmpty()) {
            binding.textOverview.visibility = View.GONE
            binding.textMore.visibility = View.GONE
            return
        }
        
        binding.textOverview.visibility = View.VISIBLE
        if (isOverviewExpanded || overview.length <= 150) {
            binding.textOverview.text = overview
            binding.textOverview.maxLines = Int.MAX_VALUE
            binding.textMore.visibility = View.GONE
        } else {
            binding.textOverview.text = overview.take(150) + "..."
            binding.textOverview.maxLines = 4
            binding.textMore.visibility = View.VISIBLE
        }
    }
    
    private fun updateGenres(genres: List<String>) {
        binding.chipGroupGenres.removeAllViews()
        genres.forEach { genreName ->
            val chip = Chip(requireContext())
            chip.text = genreName
            chip.isClickable = false
            chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                requireContext().getColor(com.example.cinephile.R.color.genre_chip_background)
            )
            chip.setTextColor(requireContext().getColor(com.example.cinephile.R.color.white))
            chip.chipCornerRadius = 16f
            binding.chipGroupGenres.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
