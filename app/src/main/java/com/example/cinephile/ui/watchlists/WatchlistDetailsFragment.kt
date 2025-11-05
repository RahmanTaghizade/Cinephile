package com.example.cinephile.ui.watchlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.cinephile.databinding.FragmentWatchlistDetailsBinding
import com.example.cinephile.R
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WatchlistDetailsFragment : Fragment() {
    private var _binding: FragmentWatchlistDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WatchlistDetailsViewModel by viewModels()

    private lateinit var adapter: WatchlistMoviesAdapter
    private var lastDeleted: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchlistDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        collectMovies()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupList() {
        adapter = WatchlistMoviesAdapter(
            onClick = { item ->
                val args = Bundle().apply { putLong("movieId", item.id) }
                findNavController().navigate(R.id.detailsFragment, args)
            },
            onDelete = { item ->
                lastDeleted = item.id
                viewModel.remove(item.id)
                Snackbar.make(requireView(), R.string.removed_from_watchlist, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) {
                        lastDeleted?.let { id -> viewModel.add(id) }
                        lastDeleted = null
                    }
                    .show()
            }
        )
        binding.recyclerMovies.adapter = adapter
    }

    private fun collectMovies() {
        val moviesState = viewModel.movies
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    moviesState.collect { list ->
                        adapter.submitList(list)
                        binding.textEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }
}
