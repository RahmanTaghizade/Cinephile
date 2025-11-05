package com.example.cinephile.ui.recommendations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cinephile.databinding.FragmentRecommendationsBinding
import com.example.cinephile.ui.search.MovieAdapter

class RecommendationsFragment : Fragment() {
    private var _binding: FragmentRecommendationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var latestAdapter: MovieAdapter
    private lateinit var upcomingAdapter: MovieAdapter
    private lateinit var continueAdapter: MovieAdapter

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
        setupCategoryChips()
        setupSeeAllClick()
    }

    private fun setupRecyclerViews() {
        // Latest movies - horizontal scrolling
        latestAdapter = MovieAdapter(
            onItemClick = { movieId ->
                // TODO: Navigate to details
            },
            onLongPress = { movieId ->
                // TODO: Add to watchlist
            }
        )
        binding.rvLatest.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = latestAdapter
        }

        // Upcoming movies - horizontal scrolling
        upcomingAdapter = MovieAdapter(
            onItemClick = { movieId ->
                // TODO: Navigate to details
            },
            onLongPress = { movieId ->
                // TODO: Add to watchlist
            }
        )
        binding.rvUpcoming.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = upcomingAdapter
        }

        // Continue watching - horizontal scrolling
        continueAdapter = MovieAdapter(
            onItemClick = { movieId ->
                // TODO: Navigate to details
            },
            onLongPress = { movieId ->
                // TODO: Add to watchlist
            }
        )
        binding.rvContinue.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = continueAdapter
        }

        // Initialize with empty lists for now
        latestAdapter.submitList(emptyList())
        upcomingAdapter.submitList(emptyList())
        continueAdapter.submitList(emptyList())
    }

    private fun setupCategoryChips() {
        // Category chips are already in XML, but we can add click listeners if needed
        // The chips will be managed by the fragment/ViewModel when data is available
    }

    private fun setupSeeAllClick() {
        binding.actionLatestSeeAll.setOnClickListener {
            // TODO: Navigate to full list of latest movies
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
