package com.example.cinephile.ui.watchlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cinephile.R
import com.example.cinephile.databinding.FragmentWatchlistDetailsBinding
import com.example.cinephile.ui.search.MovieAdapter
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

    private lateinit var adapter: MovieAdapter

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
        
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.headerContainer) { container, insets ->
            val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            container.updatePadding(top = statusBarInset)
            insets
        }
        
        setupBackButton()
        setupList()
        setupSettingsMenu()
        setupResultListeners()
        collectWatchlist()
        collectMovies()
        collectConnectivity()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupBackButton() {
        binding.buttonBack.setOnClickListener {
            if (!findNavController().navigateUp()) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setupList() {
        adapter = MovieAdapter(
            onItemClick = { movie ->
                if (movie.isSeries) {
                    val args = Bundle().apply { putLong("seriesId", movie.id) }
                    findNavController().navigate(R.id.seriesDetailsFragment, args)
                } else {
                    val args = Bundle().apply { putLong("movieId", movie.id) }
                    findNavController().navigate(R.id.detailsFragment, args)
                }
            }
        )
        binding.recyclerMovies.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@WatchlistDetailsFragment.adapter
            setHasFixedSize(false)
        }
    }

    private fun setupSettingsMenu() {
        binding.buttonSettings.setOnClickListener {
            val popup = android.widget.PopupMenu(requireContext(), it)
            popup.menuInflater.inflate(R.menu.menu_watchlist_details, popup.menu)
            
            val watchlist = viewModel.watchlist.value
            val makeCurrentItem = popup.menu.findItem(R.id.action_make_current)
            makeCurrentItem?.isEnabled = watchlist?.isCurrent != true
            
            popup.setOnMenuItemClickListener { menuItem ->
                handleMenuAction(menuItem)
                true
            }
            popup.show()
        }
    }

    private fun handleMenuAction(menuItem: MenuItem): Boolean {
        val watchlist = viewModel.watchlist.value ?: return false
        return when (menuItem.itemId) {
            R.id.action_make_current -> {
                viewModel.makeCurrent()
                Snackbar.make(binding.root, getString(R.string.watchlist_set_current, watchlist.name), Snackbar.LENGTH_SHORT).show()
                true
            }
            R.id.action_rename -> {
                RenameWatchlistBottomSheet.newInstance(watchlist.id, watchlist.name)
                    .show(childFragmentManager, "RenameWatchlistBottomSheet")
                true
            }
            R.id.action_delete -> {
                DeleteWatchlistBottomSheet.newInstance(watchlist.id)
                    .show(childFragmentManager, "DeleteWatchlistBottomSheet")
                true
            }
            else -> false
        }
    }

    private fun setupResultListeners() {
        
        childFragmentManager.setFragmentResultListener(
            RenameWatchlistBottomSheet.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val name = bundle.getString(RenameWatchlistBottomSheet.ARG_NAME) ?: return@setFragmentResultListener
            viewModel.rename(name)
        }

        
        childFragmentManager.setFragmentResultListener(
            DeleteWatchlistBottomSheet.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val id = bundle.getLong(DeleteWatchlistBottomSheet.ARG_ID, -1L)
            if (id > 0L) {
                viewModel.delete()
                
                if (!findNavController().navigateUp()) {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    private fun collectWatchlist() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.watchlist.collect { watchlist ->
                        binding.textTitle.text = watchlist?.name ?: ""
                    }
                }
            }
        }
    }

    private fun collectMovies() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.movies.collect { list ->
                        adapter.submitList(list)
                        
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
