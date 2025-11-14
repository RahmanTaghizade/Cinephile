package com.example.cinephile.ui.actor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
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
import com.example.cinephile.databinding.FragmentActorProfileBinding
import com.example.cinephile.domain.repository.PersonCreditType
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@AndroidEntryPoint
class ActorProfileFragment : Fragment() {

    private var _binding: FragmentActorProfileBinding? = null
    private val binding get() = _binding!!
    private val args: ActorProfileFragmentArgs by navArgs()
    private val viewModel: ActorProfileViewModel by viewModels()

    private val moviesAdapter = ActorMoviesAdapter(::onMovieClicked)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActorProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { toolbar, insets ->
            val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            toolbar.updatePadding(top = statusBarInset)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.recyclerMovies.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = moviesAdapter
        }

        binding.buttonRetry.setOnClickListener { viewModel.reload() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: ActorProfileUiState) {
        binding.progressBar.isVisible = state.isLoading
        binding.errorGroup.isVisible = state.error != null
        binding.scrollView.isVisible = state.error == null && !state.isLoading

        state.error?.let {
            binding.textError.text = it
            return
        }

        binding.toolbar.title = state.profile?.name ?: state.toolbarTitle ?: ""

        state.profile?.let { profile ->
            binding.imageBackdrop.load(profile.profileImageUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
            }

            binding.textName.text = profile.name

            binding.chipGroupRoles.removeAllViews()
            profile.roles.forEach { role ->
                val chip = Chip(requireContext()).apply {
                    text = role
                    isCheckable = false
                    isClickable = false
                }
                binding.chipGroupRoles.addView(chip)
            }
            binding.chipGroupRoles.isVisible = profile.roles.isNotEmpty()

            binding.containerInfo.removeAllViews()
            val infoRows = mutableListOf<Pair<String, String>>()
            if (profile.birthDateDisplay != null) {
                val ageSuffix = profile.ageYears?.let { years ->
                    resources.getQuantityString(R.plurals.actor_age_years, years, years)
                }
                val value = listOfNotNull(profile.birthDateDisplay, ageSuffix).joinToString(" • ")
                infoRows.add(getString(R.string.actor_birth_date) to value)
            }
            profile.placeOfBirth?.let {
                infoRows.add(getString(R.string.actor_birth_place) to it)
            }
            profile.deathDateDisplay?.let {
                infoRows.add(getString(R.string.actor_death_date) to it)
            }
            if (profile.alsoKnownAs.isNotEmpty()) {
                infoRows.add(getString(R.string.actor_also_known) to profile.alsoKnownAs.joinToString(", "))
            }

            infoRows.forEach { (label, value) -> addInfoRow(label, value) }
            binding.cardInfo.isVisible = infoRows.isNotEmpty()

            if (!profile.biography.isNullOrBlank()) {
                binding.textBiographyLabel.isVisible = true
                binding.textBiography.isVisible = true
                binding.textBiography.text = profile.biography
            } else {
                binding.textBiographyLabel.isVisible = false
                binding.textBiography.isVisible = false
            }
        }

        val movies = state.movies
        moviesAdapter.submitList(movies)
        val hasMovies = movies.isNotEmpty()
        binding.textFilmographyLabel.isVisible = hasMovies
        binding.recyclerMovies.isVisible = hasMovies
        binding.textEmptyMovies.isVisible = !hasMovies && state.profile != null && !state.isLoading
    }

    private fun addInfoRow(label: String, value: String) {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val spacingPx = (resources.displayMetrics.density * 12f).roundToInt()
                bottomMargin = spacingPx
            }
        }

        val labelView = TextView(context).apply {
            text = label
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            setTextColor(requireContext().getColor(R.color.text_secondary))
        }

        val valueView = TextView(context).apply {
            text = value
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
            setTextColor(requireContext().getColor(R.color.text_primary))
        }

        container.addView(labelView)
        container.addView(valueView)
        binding.containerInfo.addView(container)
    }

    private fun onMovieClicked(movieId: Long) {
        val action = ActorProfileFragmentDirections.actionActorProfileFragmentToDetailsFragment(movieId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

