package com.example.cinephile.ui.watchlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
        setupRetryButton()
        collectFlows()
    }
    
    private fun setupRetryButton() {
        binding.buttonRetry.setOnClickListener {
            viewModel.retry()
        }
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
            onDelete = { item -> showDeleteConfirm(item.id) }
        )
        binding.recyclerWatchlists.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
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
        val context = requireContext()
        val inputLayout = TextInputLayout(context).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setPadding(
                resources.getDimensionPixelSize(R.dimen.dialog_padding_horizontal),
                resources.getDimensionPixelSize(R.dimen.dialog_padding_vertical),
                resources.getDimensionPixelSize(R.dimen.dialog_padding_horizontal),
                0
            )
            hint = getString(R.string.dialog_rename_hint)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val editText = TextInputEditText(context).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setText(currentName)
        }
        inputLayout.addView(
            editText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        
        MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Cinephile_AlertDialog)
            .setTitle(R.string.dialog_rename_title)
            .setView(inputLayout)
            .setPositiveButton(R.string.rename) { _, _ ->
                val name = editText.text?.toString() ?: return@setPositiveButton
                viewModel.rename(id, name)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteConfirm(id: Long) {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Cinephile_AlertDialog)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.delete(id) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}

