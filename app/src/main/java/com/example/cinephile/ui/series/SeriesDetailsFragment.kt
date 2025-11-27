package com.example.cinephile.ui.series

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.cinephile.R
import com.example.cinephile.databinding.FragmentDetailsBinding
import com.example.cinephile.ui.details.CastAdapter
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SeriesDetailsFragment : Fragment() {
    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!
    private val args: SeriesDetailsFragmentArgs by navArgs()
    private val viewModel: SeriesDetailsViewModel by viewModels()
    private val castAdapter = CastAdapter(::onCastClicked)
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { toolbar, insets ->
            val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            toolbar.updatePadding(top = statusBarInset)
            insets
        }

        binding.recyclerCast.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerCast.adapter = castAdapter

        binding.recyclerSimilar.visibility = View.GONE
        binding.textSimilarLabel.visibility = View.GONE

        binding.textMore.setOnClickListener {
            isOverviewExpanded = !isOverviewExpanded
            updateOverviewText()
        }

        binding.buttonRetry.setOnClickListener {
            viewModel.retry()
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                binding.progressLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                val hasError = state.error != null && !state.isLoading
                binding.errorCard.visibility = if (hasError) View.VISIBLE else View.GONE
                binding.scrollContent.visibility = if (hasError) View.GONE else View.VISIBLE
                if (hasError) {
                    binding.textError.text = state.error
                }

                val isEmpty = state.title == null && !state.isLoading && state.error == null
                binding.textEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.scrollContent.visibility = if (isEmpty) View.GONE else View.VISIBLE

                if (state.title != null) {
                    updateUI(state)
                }
                }
            }
        }
    }

    private fun updateUI(state: SeriesDetailsUiState) {
        binding.toolbar.title = state.title

        state.posterUrl?.let { url ->
            binding.imagePoster.load(url) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder_movie)
                error(R.drawable.ic_placeholder_movie)
            }
        }

        binding.textTitle.text = state.title
        
        val rating = state.voteAverage
        binding.textRating.text = if (rating > 0) {
            "%.1f ⭐".format(rating)
        } else {
            ""
        }
        
        val metadataText = buildString {
            if (state.firstAirDate != null) {
                append(state.firstAirDate.take(4))
            }
            if (state.runtime.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append(state.runtime)
            }
            if (state.numberOfSeasons > 0) {
                if (isNotEmpty()) append(" • ")
                append("${state.numberOfSeasons} season${if (state.numberOfSeasons > 1) "s" else ""}")
            }
        }
        binding.textMetadata.text = metadataText.ifEmpty { "" }
        
        binding.textOverview.text = state.overview ?: "No overview available"
        updateOverviewText()

        binding.chipGroupGenres.removeAllViews()
        if (state.genres.isNotEmpty()) {
            state.genres.forEach { genreName ->
                val chip = Chip(requireContext()).apply {
                    text = genreName
                    isClickable = false
                    chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                        requireContext().getColor(R.color.genre_chip_background)
                    )
                    setTextColor(requireContext().getColor(R.color.text_primary))
                }
                binding.chipGroupGenres.addView(chip)
            }
        }

        castAdapter.submitList(state.cast)
        binding.recyclerCast.visibility = if (state.cast.isNotEmpty()) View.VISIBLE else View.GONE
        binding.textCastLabel.visibility = if (state.cast.isNotEmpty()) View.VISIBLE else View.GONE

        binding.buttonWatchlist.visibility = View.VISIBLE
        binding.buttonWatchlist.text = if (state.isInWatchlist) {
            getString(R.string.watchlist_remove_button)
        } else {
            getString(R.string.watchlist_add_button)
        }
        binding.buttonWatchlist.setOnClickListener {
            viewModel.toggleWatchlist()
        }
        
        binding.ratingBar.visibility = View.GONE
        binding.textYourRatingLabel.visibility = View.GONE
        
        state.snackbarMessage?.let { message ->
            com.google.android.material.snackbar.Snackbar.make(binding.root, message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            viewModel.clearSnackbarMessage()
        }
    }

    private fun updateOverviewText() {
        val overview = binding.textOverview.text.toString()
        if (overview.length > MAX_OVERVIEW_LENGTH && !isOverviewExpanded) {
            val truncated = overview.take(MAX_OVERVIEW_LENGTH) + "..."
            binding.textOverview.text = truncated
            binding.textMore.visibility = View.VISIBLE
        } else {
            binding.textOverview.text = viewModel.uiState.value.overview ?: "No overview available"
            binding.textMore.visibility = View.GONE
        }
    }

    private fun onCastClicked(castMember: com.example.cinephile.ui.search.CastMember) {
        val action = com.example.cinephile.ui.search.SearchFragmentDirections
            .actionSearchFragmentToActorProfileFragment(
                actorId = castMember.id,
                actorName = castMember.name
            )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MAX_OVERVIEW_LENGTH = 200
    }
}

