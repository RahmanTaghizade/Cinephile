package com.example.cinephile.ui.quiz

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cinephile.R
import com.example.cinephile.databinding.FragmentQuizDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuizDetailsFragment : Fragment() {
    private var _binding: FragmentQuizDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizDetailsViewModel by viewModels()

    private lateinit var resultsAdapter: QuizResultsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupStartButton()
        setupRetryButton()
        collectQuizData()
        collectResults()
    }
    
    private fun setupRetryButton() {
        binding.buttonRetry.setOnClickListener {
            // Retry loading quiz data
            viewModel.retry()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        resultsAdapter = QuizResultsAdapter()
        binding.recyclerResults.apply {
            adapter = resultsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupStartButton() {
        binding.buttonStart.setOnClickListener {
            val quizId = viewModel.quiz.value?.id ?: return@setOnClickListener
            val args = Bundle().apply {
                putLong("quizId", quizId)
            }
            findNavController().navigate(R.id.action_quizDetailsFragment_to_quizPlayFragment, args)
        }
    }

    private fun collectQuizData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.quiz.collect { quiz ->
                        val isLoading = quiz == null && viewModel.isLoading.value
                        val hasError = viewModel.error.value != null && !isLoading
                        
                        binding.progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
                        binding.errorCard.visibility = if (hasError) View.VISIBLE else View.GONE
                        binding.scrollContent.visibility = if (isLoading || hasError) View.GONE else View.VISIBLE
                        
                        if (hasError) {
                            binding.textError.text = viewModel.error.value
                        }
                        
                        if (quiz != null) {
                            binding.textQuizName.text = quiz.name
                            binding.textWatchlistName.text = "Watchlist: ${quiz.watchlistName}"
                            binding.textDifficulty.text = "Difficulty: ${quiz.difficulty.name}"
                            binding.textMode.text = "Mode: ${quiz.mode.name}"
                        }
                    }
                }
                launch {
                    viewModel.questionCount.collect { count ->
                        binding.textQuestionCount.text = "Questions: $count"
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
        val quiz = viewModel.quiz.value
        val isLoading = viewModel.isLoading.value
        val error = viewModel.error.value
        
        binding.progressLoading.visibility = if (isLoading && quiz == null) View.VISIBLE else View.GONE
        binding.errorCard.visibility = if (error != null && !isLoading) View.VISIBLE else View.GONE
        binding.scrollContent.visibility = if (isLoading || (error != null && quiz == null)) View.GONE else View.VISIBLE
        
        if (error != null) {
            binding.textError.text = error
        }
    }

    private fun collectResults() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.results.collect { results ->
                        resultsAdapter.submitList(results)
                        val isEmpty = results.isEmpty()
                        binding.textNoResults.visibility = if (isEmpty) View.VISIBLE else View.GONE
                        binding.recyclerResults.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }
}
