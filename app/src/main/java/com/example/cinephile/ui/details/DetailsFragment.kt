package com.example.cinephile.ui.details

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.cinephile.R
import com.example.cinephile.databinding.FragmentDetailsBinding
import com.example.cinephile.ui.details.DetailsEvent.ShowWatchlistPicker
import com.example.cinephile.domain.repository.WatchlistUiModel
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import androidx.navigation.navOptions
import com.example.cinephile.ui.search.CastMember
import com.example.cinephile.ui.details.DetailsFragmentDirections

@AndroidEntryPoint
class DetailsFragment : Fragment() {
    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!
    private val args: DetailsFragmentArgs by navArgs()
    private val viewModel: DetailsViewModel by viewModels()
    private val castAdapter = CastAdapter(::onCastClicked)
    private val similarAdapter = SimilarMoviesAdapter(::onSimilarMovieClicked)
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
        
        // Setup cast RecyclerView
        binding.recyclerCast.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerCast.adapter = castAdapter

        binding.recyclerSimilar.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerSimilar.adapter = similarAdapter
        
        // Setup "more" link click listener
        binding.textMore.setOnClickListener {
            isOverviewExpanded = !isOverviewExpanded
            updateOverviewText()
        }
        
        // Setup retry button
        binding.buttonRetry.setOnClickListener {
            viewModel.load()
        }
        
        // Observe UI state
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                // Handle loading state
                binding.progressLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                
                // Handle error state
                val hasError = state.error != null && !state.isLoading
                binding.errorCard.visibility = if (hasError) View.VISIBLE else View.GONE
                binding.scrollContent.visibility = if (hasError) View.GONE else View.VISIBLE
                if (hasError) {
                    binding.textError.text = state.error
                }
                
                // Handle empty state (when no movie data)
                val isEmpty = state.movie == null && !state.isLoading && state.error == null
                binding.textEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.scrollContent.visibility = if (isEmpty) View.GONE else View.VISIBLE
                
                // Only update content if we have data
                if (state.movie == null && !state.isLoading) {
                    return@collect
                }
                
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

                binding.buttonWatchlist.text = if (state.isInWatchlist) {
                    getString(R.string.watchlist_remove_button)
                } else {
                    getString(R.string.watchlist_add_button)
                }
                
                // Update overview
                updateOverviewText()
                
                // Update genres
                updateGenres(movie?.genres ?: emptyList())
                
                // Update cast
                castAdapter.submitList(movie?.cast ?: emptyList())
                
                // Update similar movies
                similarAdapter.submitList(state.similarMovies)
                val hasSimilar = state.similarMovies.isNotEmpty()
                binding.textSimilarLabel.isVisible = hasSimilar
                binding.recyclerSimilar.isVisible = hasSimilar

                // Update poster
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
            if (!findNavController().navigateUp()) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.buttonWatchlist.setOnClickListener { viewModel.toggleWatchlist() }

        observeEvents()
    }

    private fun onCastClicked(castMember: CastMember) {
        val action = DetailsFragmentDirections.actionDetailsFragmentToActorProfileFragment(
            actorId = castMember.id,
            actorName = castMember.name
        )
        findNavController().navigate(action)
    }

    private fun onSimilarMovieClicked(movieId: Long) {
        if (movieId == args.movieId) return
        val bundle = bundleOf("movieId" to movieId)
        val options = navOptions { launchSingleTop = true }
        findNavController().navigate(R.id.detailsFragment, bundle, options)
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

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is ShowWatchlistPicker -> showWatchlistPicker(event.watchlists)
                }
            }
        }
    }

    private fun showWatchlistPicker(watchlists: List<WatchlistUiModel>) {
        val options = watchlists.map { it.name as CharSequence } +
            getString(R.string.watchlist_picker_create) as CharSequence
        val items: Array<CharSequence> = options.toTypedArray()
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.watchlist_picker_title)
            .setItems(items) { _, which ->
                if (which == watchlists.size) {
                    showCreateWatchlistDialog()
                } else {
                    watchlists.getOrNull(which)?.let { selected ->
                        viewModel.confirmAddToWatchlist(selected.id)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_watchlist_dialog)
        dialog.show()
    }

    private fun showCreateWatchlistDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_watchlist, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.edit_text_name)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.watchlist_create_title)
            .setView(dialogView)
            .setPositiveButton(R.string.watchlist_create_confirm) { _, _ ->
                val name = editText.text?.toString().orEmpty()
                viewModel.createWatchlistAndAdd(name)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_watchlist_dialog)
        dialog.show()
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
