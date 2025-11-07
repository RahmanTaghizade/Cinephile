package com.example.cinephile.ui.quiz

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.cinephile.R
import com.example.cinephile.databinding.DialogCreateQuizBinding
import com.example.cinephile.domain.repository.QuizDifficulty
import com.example.cinephile.domain.repository.QuizMode
import com.example.cinephile.domain.repository.WatchlistRepository
import com.example.cinephile.domain.repository.WatchlistUiModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class CreateQuizDialogFragment : DialogFragment() {

    @Inject
    lateinit var watchlistRepository: WatchlistRepository

    private var _binding: DialogCreateQuizBinding? = null
    private val binding get() = _binding!!

    private var watchlists: List<WatchlistUiModel> = emptyList()
    private var selectedWatchlistId: Long? = null

    var onCreateQuiz: (suspend (String, Long, Int, QuizDifficulty, QuizMode) -> Long)? = null
    var onQuizCreated: ((Long) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCreateQuizBinding.inflate(LayoutInflater.from(requireContext()))
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create Quiz")
            .setView(binding.root)
            .setPositiveButton("Create") { _, _ ->
                handleCreate()
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupWatchlistDropdown()
        setupDefaultName()
    }

    private fun setupWatchlistDropdown() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val flow = watchlistRepository.getAllWatchlists()
                val list = flow.first()
                watchlists = list
                if (list.isNotEmpty()) {
                    val currentWatchlist = list.find { it.isCurrent } ?: list.first()
                    selectedWatchlistId = currentWatchlist.id
                    binding.editTextWatchlist.setText(currentWatchlist.name)
                }
                
                binding.editTextWatchlist.setOnClickListener {
                    showWatchlistPicker()
                }
            } catch (e: Exception) {
                // Handle error - could show toast or log
            }
        }
    }

    private fun setupDefaultName() {
        viewLifecycleOwner.lifecycleScope.launch {
            val current = watchlistRepository.getCurrentWatchlist().first()
            if (current != null && binding.editTextName.text.isNullOrBlank()) {
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val defaultName = "Quiz ${current.name} ${dateFormat.format(System.currentTimeMillis())}"
                binding.editTextName.setText(defaultName)
            }
        }
    }

    private fun showWatchlistPicker() {
        if (watchlists.isEmpty()) {
            Toast.makeText(requireContext(), "No watchlists available", Toast.LENGTH_SHORT).show()
            return
        }

        val items = watchlists.map { it.name }.toTypedArray()
        val currentIndex = watchlists.indexOfFirst { it.id == selectedWatchlistId }.takeIf { it >= 0 } ?: 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Watchlist")
            .setSingleChoiceItems(items, currentIndex) { dialog, which ->
                val selected = watchlists[which]
                selectedWatchlistId = selected.id
                binding.editTextWatchlist.setText(selected.name)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleCreate() {
        val name = binding.editTextName.text.toString().trim()
        val questionCountText = binding.editTextQuestionCount.text.toString().trim()
        val questionCount = questionCountText.toIntOrNull() ?: 10

        if (name.isBlank()) {
            Toast.makeText(requireContext(), "Please enter a quiz name", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedWatchlistId == null) {
            Toast.makeText(requireContext(), "Please select a watchlist", Toast.LENGTH_SHORT).show()
            return
        }

        if (questionCount < 5) {
            Toast.makeText(requireContext(), "Question count must be at least 5", Toast.LENGTH_SHORT).show()
            return
        }

        val difficulty = when (binding.chipGroupDifficulty.checkedChipId) {
            R.id.chip_easy -> QuizDifficulty.EASY
            R.id.chip_medium -> QuizDifficulty.MEDIUM
            R.id.chip_hard -> QuizDifficulty.HARD
            else -> QuizDifficulty.EASY
        }

        val mode = when (binding.chipGroupMode.checkedChipId) {
            R.id.chip_timed -> QuizMode.TIMED
            R.id.chip_survival -> QuizMode.SURVIVAL
            else -> QuizMode.TIMED
        }

        val createQuiz = onCreateQuiz
        if (createQuiz == null) {
            Toast.makeText(requireContext(), "Create quiz callback not set", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val quizId = createQuiz(name, selectedWatchlistId!!, questionCount, difficulty, mode)
                onQuizCreated?.invoke(quizId)
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error creating quiz: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

