package com.example.cinephile.ui.quiz

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.cinephile.R
import com.example.cinephile.databinding.DialogCreateQuizBinding
import com.example.cinephile.domain.repository.QuizRepository
import com.example.cinephile.domain.repository.WatchlistRepository
import com.example.cinephile.domain.repository.WatchlistUiModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class CreateQuizDialogFragment : DialogFragment() {

    companion object {
        private const val TAG = "CreateQuizDialog"
    }

    @Inject
    lateinit var watchlistRepository: WatchlistRepository
    
    @Inject
    lateinit var quizRepository: QuizRepository

    private var _binding: DialogCreateQuizBinding? = null
    private val binding get() = _binding!!

    private var watchlists: List<WatchlistUiModel> = emptyList()
    private var selectedWatchlistId: Long? = null

    var onCreateQuiz: (suspend (String, Long, Int, com.example.cinephile.domain.repository.QuizDifficulty, com.example.cinephile.domain.repository.QuizMode) -> Long)? = null
    var onQuizCreated: ((Long) -> Unit)? = null
    
    private var selectedDifficulty: com.example.cinephile.domain.repository.QuizDifficulty = com.example.cinephile.domain.repository.QuizDifficulty.EASY
    private var selectedMode: com.example.cinephile.domain.repository.QuizMode = com.example.cinephile.domain.repository.QuizMode.TIMED

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCreateQuizBinding.inflate(LayoutInflater.from(requireContext()))
        
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Cinephile)
            .setTitle("Create Quiz")
            .setView(binding.root)
            .setPositiveButton("Create", null)
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(Dialog.BUTTON_NEGATIVE)
            
            positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_dark_primary))
            negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_dark_onSurfaceVariant))
            
            positiveButton.setOnClickListener {
                handleCreate()
            }
        }
        
        
        setupWatchlistDropdown()
        setupDifficultyDropdown()
        setupModeDropdown()
        setupDefaultName()
        
        return dialog
    }
    
    init {
        
    }

    private fun setupWatchlistDropdown() {
        Log.d(TAG, "setupWatchlistDropdown: Starting")
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "setupWatchlistDropdown: Fetching watchlists from repository")
                val watchlistsList = watchlistRepository.getAllWatchlists().first()
                Log.d(TAG, "setupWatchlistDropdown: Received ${watchlistsList.size} watchlists")
                
                watchlists = watchlistsList
                
                if (watchlistsList.isEmpty()) {
                    Log.w(TAG, "setupWatchlistDropdown: No watchlists found")
                    binding.textInputLayoutWatchlist.isEnabled = false
                    binding.textInputLayoutWatchlist.hint = "No watchlists available"
                    Toast.makeText(requireContext(), "Please create a watchlist first", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                
                val watchlistNames = watchlistsList.map { it.name }
                Log.d(TAG, "setupWatchlistDropdown: Watchlist names: $watchlistNames")
                
                
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    watchlistNames
                )
                
                
                val autoCompleteTextView = binding.editTextWatchlist as AutoCompleteTextView
                
                
                autoCompleteTextView.setAdapter(adapter)
                autoCompleteTextView.threshold = 1
                autoCompleteTextView.isFocusable = false
                autoCompleteTextView.isClickable = true
                autoCompleteTextView.isFocusableInTouchMode = false
                
                
                val defaultWatchlist = watchlistsList.find { it.isCurrent } ?: watchlistsList.first()
                selectedWatchlistId = defaultWatchlist.id
                autoCompleteTextView.setText(defaultWatchlist.name, false)
                Log.d(TAG, "setupWatchlistDropdown: Default watchlist set: ${defaultWatchlist.name} (id=${defaultWatchlist.id})")
                
                
                autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                    if (position < watchlistsList.size) {
                        val selected = watchlistsList[position]
                        selectedWatchlistId = selected.id
                        Log.d(TAG, "setupWatchlistDropdown: Selected watchlist: ${selected.name} (id=${selected.id})")
                        binding.textInputLayoutWatchlist.error = null
                    }
                }
                
                
                autoCompleteTextView.setOnClickListener {
                    Log.d(TAG, "setupWatchlistDropdown: AutoCompleteTextView clicked, showing dropdown")
                    autoCompleteTextView.showDropDown()
                }
                
                
                binding.textInputLayoutWatchlist.setEndIconOnClickListener {
                    Log.d(TAG, "setupWatchlistDropdown: End icon clicked, showing dropdown")
                    autoCompleteTextView.showDropDown()
                }
                
                Log.d(TAG, "setupWatchlistDropdown: Setup complete. Adapter count: ${adapter.count}")
                
            } catch (e: Exception) {
                Log.e(TAG, "setupWatchlistDropdown: Error", e)
                Toast.makeText(requireContext(), "Error loading watchlists: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDifficultyDropdown() {
        val difficulties = listOf("Easy", "Medium", "Hard")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            difficulties
        )
        
        val autoCompleteTextView = binding.editTextDifficulty as AutoCompleteTextView
        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.setText("Easy", false)
        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            selectedDifficulty = when (position) {
                0 -> com.example.cinephile.domain.repository.QuizDifficulty.EASY
                1 -> com.example.cinephile.domain.repository.QuizDifficulty.MEDIUM
                2 -> com.example.cinephile.domain.repository.QuizDifficulty.HARD
                else -> com.example.cinephile.domain.repository.QuizDifficulty.EASY
            }
        }
        autoCompleteTextView.setOnClickListener {
            autoCompleteTextView.showDropDown()
        }
    }

    private fun setupModeDropdown() {
        val modes = listOf("Timed", "Survival")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            modes
        )
        
        val autoCompleteTextView = binding.editTextMode as AutoCompleteTextView
        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.setText("Timed", false)
        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            selectedMode = when (position) {
                0 -> com.example.cinephile.domain.repository.QuizMode.TIMED
                1 -> com.example.cinephile.domain.repository.QuizMode.SURVIVAL
                else -> com.example.cinephile.domain.repository.QuizMode.TIMED
            }
        }
        autoCompleteTextView.setOnClickListener {
            autoCompleteTextView.showDropDown()
        }
    }

    private fun setupDefaultName() {
        lifecycleScope.launch {
            try {
                val current = watchlistRepository.getCurrentWatchlist().first()
                if (current != null && binding.editTextName.text.isNullOrBlank()) {
                    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val defaultName = "Quiz ${current.name} ${dateFormat.format(System.currentTimeMillis())}"
                    binding.editTextName.setText(defaultName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "setupDefaultName: Error", e)
            }
        }
    }

    private fun handleCreate() {
        val name = binding.editTextName.text.toString().trim()
        val questionCountText = binding.editTextQuestionCount.text.toString().trim()
        val questionCount = questionCountText.toIntOrNull() ?: 10

        if (name.isBlank()) {
            binding.textInputLayoutName.error = "Please enter a quiz name"
            return
        } else {
            binding.textInputLayoutName.error = null
        }

        if (selectedWatchlistId == null) {
            binding.textInputLayoutWatchlist.error = "Please select a watchlist"
            return
        } else {
            binding.textInputLayoutWatchlist.error = null
        }

        if (questionCount < 1) {
            binding.textInputLayoutQuestionCount.error = "Question count must be at least 1"
            return
        } else {
            binding.textInputLayoutQuestionCount.error = null
        }

        val createQuiz = onCreateQuiz
        if (createQuiz == null) {
            Toast.makeText(requireContext(), "Create quiz callback not set", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val movieCount = quizRepository.getWatchlistMovieCount(selectedWatchlistId!!)
                if (movieCount < 4) {
                    binding.textInputLayoutWatchlist.error = "Watchlist must have at least 4 movies"
                    return@launch
                }
                
                val quizId = createQuiz(name, selectedWatchlistId!!, questionCount, selectedDifficulty, selectedMode)
                onQuizCreated?.invoke(quizId)
                dialog?.dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "handleCreate: Error", e)
                Toast.makeText(requireContext(), "Error creating quiz: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
