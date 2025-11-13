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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.cinephile.databinding.FragmentWatchlistDetailsBinding
import com.example.cinephile.R
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.cinephile.util.ConnectivityMonitor

@AndroidEntryPoint
class WatchlistDetailsFragment : Fragment() {
    private var _binding: FragmentWatchlistDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WatchlistDetailsViewModel by viewModels()
    @Inject lateinit var connectivityMonitor: ConnectivityMonitor

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
        collectConnectivity()
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
        binding.recyclerMovies.apply {
            adapter = this@WatchlistDetailsFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun collectMovies() {
        val moviesState = viewModel.movies
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    moviesState.collect { list ->
                        adapter.submitList(list)
                        // Handle empty state
                        val isEmpty = list.isEmpty()
                        binding.textEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                        binding.recyclerMovies.visibility = if (isEmpty) View.GONE else View.VISIBLE
                        binding.progressLoading.visibility = View.GONE
                        binding.errorCard.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun collectConnectivity() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    connectivityMonitor.isOnlineFlow.collect { isOnline ->
                        binding.offlineBanner.visibility = if (isOnline) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }
}
