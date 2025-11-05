package com.example.cinephile.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.cinephile.databinding.FragmentDetailsBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetailsFragment : Fragment() {
    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!
    private val args: DetailsFragmentArgs by navArgs()
    private val viewModel: DetailsViewModel by viewModels()

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
        // Observe UI state
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                binding.toolbar.title = state.title ?: getString(com.example.cinephile.R.string.app_name)
                binding.textMovieId.text = "movieId: ${args.movieId}"
                binding.textTitle.text = state.title ?: ""
                binding.textDirector.text = state.director?.let { "Director: $it" } ?: ""
                binding.textReleaseDate.text = state.releaseDate?.let { "Release: $it" } ?: ""
                binding.textRuntime.text = state.movie?.let { m ->
                    // runtime isn't in MovieUiModel; leave blank for now
                    ""
                } ?: ""
                binding.textOverview.text = state.movie?.let { _ -> binding.textOverview.text }?.toString() ?: binding.textOverview.text.toString()
                binding.ratingBar.rating = state.userRating

                // Update watchlist button label
                binding.buttonWatchlist.text = if (state.isInWatchlist) {
                    "Remove from Watchlist"
                } else {
                    "Add to Watchlist"
                }

                binding.imagePoster.load(state.posterUrl) {
                    crossfade(true)
                }
                
                // Show Snackbar if there's a message
                state.snackbarMessage?.let { message ->
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    viewModel.clearSnackbarMessage()
                }
            }
        }

        // Toolbar back navigation
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.buttonFavorite.setOnClickListener { viewModel.toggleFavorite() }
        binding.ratingBar.setOnRatingBarChangeListener { _, rating, _ -> viewModel.setRating(rating) }
        binding.buttonWatchlist.setOnClickListener { viewModel.toggleWatchlist() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
