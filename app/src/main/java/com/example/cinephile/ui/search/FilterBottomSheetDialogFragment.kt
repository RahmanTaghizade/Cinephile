package com.example.cinephile.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.cinephile.R
import com.example.cinephile.databinding.BottomSheetFiltersBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FilterBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFiltersBinding? = null
    private val binding get() = _binding!!

    private val searchViewModel: SearchViewModel by lazy {
        val parent = parentFragment as? SearchFragment
            ?: throw IllegalStateException("FilterBottomSheetDialogFragment must be attached to SearchFragment")
        ViewModelProvider(parent)[SearchViewModel::class.java]
    }

    private var onFiltersApplied: ((FilterState) -> Unit)? = null

    fun setOnFiltersAppliedListener(listener: (FilterState) -> Unit) {
        onFiltersApplied = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentState = searchViewModel.searchUiState.value
        val filterState = FilterState(
            category = when (currentState.activeFilter) {
                SearchFilter.ALL -> Category.ALL
                SearchFilter.MOVIES -> Category.MOVIES
                SearchFilter.SERIES -> Category.SERIES
                else -> Category.ALL
            },
            year = currentState.selectedYear,
            ratingMin = 0f,
            ratingMax = currentState.selectedRatingMax,
            ageRestriction = currentState.selectedAgeRestriction,
            sortBy = currentState.selectedSortBy
        )

        setupUI(filterState)
        setupListeners()
    }

    private fun setupUI(state: FilterState) {
        // Category chips
        binding.chipAll.isChecked = state.category == Category.ALL
        binding.chipMovies.isChecked = state.category == Category.MOVIES
        binding.chipSeries.isChecked = state.category == Category.SERIES

        // Year
        binding.textYearValue.text = state.year?.toString() ?: "Any"

        // Countries (placeholder - will show "Any" for now)
        binding.textCountriesValue.text = "Any"

        // Rating slider
        binding.sliderRating.value = state.ratingMax
        updateRatingText(state.ratingMin, state.ratingMax)
        binding.sliderRating.addOnChangeListener { _, value, _ ->
            updateRatingText(0f, value)
        }

        // Age restriction chips
        binding.chipAge0.isChecked = state.ageRestriction == 0
        binding.chipAge6.isChecked = state.ageRestriction == 6
        binding.chipAge12.isChecked = state.ageRestriction == 12
        binding.chipAge16.isChecked = state.ageRestriction == 16
        binding.chipAge18.isChecked = state.ageRestriction == 18

        // Sort radio buttons
        binding.radioRating.isChecked = state.sortBy == SortBy.RATING
        binding.radioPopularity.isChecked = state.sortBy == SortBy.POPULARITY
        binding.radioNewness.isChecked = state.sortBy == SortBy.NEWNESS
    }

    private fun updateRatingText(min: Float, max: Float) {
        binding.textRatingValue.text = "from ${min.toInt()} to ${max.toInt()}"
    }

    private fun setupListeners() {
        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.buttonReset.setOnClickListener {
            resetFilters()
        }

        binding.layoutYear.setOnClickListener {
            // TODO: Implement year picker
        }

        binding.layoutCountries.setOnClickListener {
            // TODO: Implement country picker
        }

        binding.buttonShow.setOnClickListener {
            applyFilters()
            dismiss()
        }
    }

    private fun resetFilters() {
        val defaultState = FilterState(
            category = Category.ALL,
            year = null,
            ratingMin = 0f,
            ratingMax = 10f,
            ageRestriction = null,
            sortBy = SortBy.RATING
        )
        setupUI(defaultState)
    }

    private fun applyFilters() {
        val category = when {
            binding.chipAll.isChecked -> Category.ALL
            binding.chipMovies.isChecked -> Category.MOVIES
            binding.chipSeries.isChecked -> Category.SERIES
            else -> Category.ALL
        }

        val year = binding.textYearValue.text.toString().takeIf { it != "Any" }?.toIntOrNull()

        val ratingMax = binding.sliderRating.value

        val ageRestriction = when {
            binding.chipAge0.isChecked -> 0
            binding.chipAge6.isChecked -> 6
            binding.chipAge12.isChecked -> 12
            binding.chipAge16.isChecked -> 16
            binding.chipAge18.isChecked -> 18
            else -> null
        }

        val sortBy = when {
            binding.radioRating.isChecked -> SortBy.RATING
            binding.radioPopularity.isChecked -> SortBy.POPULARITY
            binding.radioNewness.isChecked -> SortBy.NEWNESS
            else -> SortBy.RATING
        }

        val filterState = FilterState(
            category = category,
            year = year,
            ratingMin = 0f,
            ratingMax = ratingMax,
            ageRestriction = ageRestriction,
            sortBy = sortBy
        )

        onFiltersApplied?.invoke(filterState)

        // Update ViewModel
        searchViewModel.applyFilters(filterState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FilterBottomSheetDialogFragment"
    }
}

data class FilterState(
    val category: Category,
    val year: Int?,
    val ratingMin: Float,
    val ratingMax: Float,
    val ageRestriction: Int?,
    val sortBy: SortBy
)

enum class Category {
    ALL, MOVIES, SERIES
}

enum class SortBy {
    RATING, POPULARITY, NEWNESS
}

