package com.example.cinephile.ui.watchlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.cinephile.databinding.FragmentWatchlistsBinding
import com.example.cinephile.R
import com.example.cinephile.domain.repository.WatchlistRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WatchlistsFragment : Fragment() {
    private var _binding: FragmentWatchlistsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WatchlistsViewModel by viewModels()
    @Inject lateinit var watchlistRepository: WatchlistRepository

    private lateinit var adapter: WatchlistsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchlistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        setupFab()
        setupRetryButton()
        setupCreateResultListener()
        setupRenameResultListener()
        setupDeleteResultListener()
        collectFlows()
    }
    
    private fun setupRetryButton() {
        binding.buttonRetry.setOnClickListener {
            viewModel.retry()
        }
    }

    private fun setupRenameResultListener() {
        childFragmentManager.setFragmentResultListener(
            RenameWatchlistBottomSheet.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val name = bundle.getString(RenameWatchlistBottomSheet.ARG_NAME) ?: return@setFragmentResultListener
            val id = bundle.getLong("id", -1L)
            if (id > 0L) {
                viewModel.rename(id, name)
            }
        }
    }

    private fun setupDeleteResultListener() {
        childFragmentManager.setFragmentResultListener(
            DeleteWatchlistBottomSheet.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val id = bundle.getLong(DeleteWatchlistBottomSheet.ARG_ID, -1L)
            if (id > 0L) {
                viewModel.delete(id)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupList() {
        adapter = WatchlistsAdapter(
            watchlistRepository = watchlistRepository,
            onWatchlistClick = { item ->
                val args = Bundle().apply { putLong("watchlistId", item.id) }
                findNavController().navigate(R.id.watchlistDetailsFragment, args)
            },
            onMovieClick = { movieId ->
                val args = Bundle().apply { putLong("movieId", movieId) }
                findNavController().navigate(R.id.detailsFragment, args)
            }
        )
        binding.recyclerWatchlists.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.recyclerWatchlists.adapter = adapter
    }

    private fun setupFab() {
        binding.fabNewWatchlist.setOnClickListener {
            CreateWatchlistBottomSheet.newInstance()
                .show(childFragmentManager, "CreateWatchlistBottomSheet")
        }
    }

    private fun setupCreateResultListener() {
        childFragmentManager.setFragmentResultListener(
            CreateWatchlistBottomSheet.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val name = bundle.getString(CreateWatchlistBottomSheet.ARG_NAME) ?: return@setFragmentResultListener
            viewModel.create(name)
        }
    }

    private fun collectFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { 
                    viewModel.watchlists.collect { watchlists ->
                        adapter.submitList(watchlists)
                        updateUI()
                    }
                }
                launch {
                    viewModel.isLoading.collect { updateUI() }
                }
                launch {
                    viewModel.error.collect { updateUI() }
                }
            }
        }
    }
    
    private fun updateUI() {
        val watchlists = viewModel.watchlists.value
        val isLoading = viewModel.isLoading.value
        val error = viewModel.error.value
        
        // Handle loading state
        binding.progressLoading.visibility = if (isLoading && watchlists.isEmpty()) View.VISIBLE else View.GONE
        
        // Handle error state
        val hasError = error != null && !isLoading
        binding.errorCard.visibility = if (hasError) View.VISIBLE else View.GONE
        if (hasError) {
            binding.textError.text = error
        }
        
        // Handle empty state
        val isEmpty = watchlists.isEmpty() && !isLoading && error == null
        binding.textEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        
        // Show/hide recycler view
        binding.recyclerWatchlists.visibility = if (isEmpty || isLoading || hasError) View.GONE else View.VISIBLE
    }

    private fun showRenameDialog(id: Long, currentName: String) {
        RenameWatchlistBottomSheet.newInstance(id, currentName)
            .show(childFragmentManager, "RenameWatchlistBottomSheet")
    }

    private fun showDeleteConfirm(id: Long) {
        DeleteWatchlistBottomSheet.newInstance(id)
            .show(childFragmentManager, "DeleteWatchlistBottomSheet")
    }
}

