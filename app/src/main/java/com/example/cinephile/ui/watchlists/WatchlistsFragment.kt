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
import androidx.navigation.fragment.navArgs
import com.example.cinephile.databinding.FragmentWatchlistsBinding
import com.example.cinephile.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WatchlistsFragment : Fragment() {
    private var _binding: FragmentWatchlistsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WatchlistsViewModel by viewModels()

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
        collectFlows()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupList() {
        adapter = WatchlistsAdapter(
            onItemClick = { item ->
                val args = Bundle().apply { putLong("watchlistId", item.id) }
                findNavController().navigate(R.id.watchlistDetailsFragment, args)
            },
            onRename = { item -> showRenameDialog(item.id, item.name) },
            onSetCurrent = { item -> viewModel.setCurrent(item.id) },
            onDelete = { item -> showDeleteConfirm(item.id) }
        )
        binding.recyclerWatchlists.adapter = adapter
    }

    private fun setupFab() {
        binding.fabNewWatchlist.setOnClickListener {
            viewModel.createNewWatchlist()
        }
    }

    private fun collectFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.watchlists.collect { adapter.submitList(it) } }
            }
        }
    }

    private fun showRenameDialog(id: Long, currentName: String) {
        val context = requireContext()
        val input = android.widget.EditText(context).apply {
            setText(currentName)
            hint = getString(R.string.dialog_rename_hint)
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_rename_title)
            .setView(input)
            .setPositiveButton(R.string.rename) { _, _ ->
                val name = input.text?.toString() ?: return@setPositiveButton
                viewModel.rename(id, name)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteConfirm(id: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.delete(id) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
