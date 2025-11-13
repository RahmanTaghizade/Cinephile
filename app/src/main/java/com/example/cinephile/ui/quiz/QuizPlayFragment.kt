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
import com.example.cinephile.R
import com.example.cinephile.databinding.FragmentQuizPlayBinding
import com.example.cinephile.domain.repository.QuestionType
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuizPlayFragment : Fragment() {
    private var _binding: FragmentQuizPlayBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizPlayViewModel by viewModels()

    private val optionButtons = mutableListOf<MaterialButton>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizPlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButton()
        observeState()
        observeEvents()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupButton() {
        binding.buttonNext.setOnClickListener {
            viewModel.submitAnswer()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        updateUI(state)
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is QuizPlayEvent.QuizFinished -> {
                                showSummaryDialog()
                            }
                            else -> { /* Other events handled in state updates */ }
                        }
                    }
                }
            }
        }
    }

    private fun updateUI(state: QuizPlayState) {
        val currentQuestion = state.questions.getOrNull(state.currentIndex)
        
        if (currentQuestion == null || state.isFinished) {
            return
        }

        // Update question number
        binding.textQuestionNumber.text = "Q ${state.currentIndex + 1}/${state.questions.size}"

        // Update timer
        binding.textTimer.text = state.timeRemaining.toString()

        // Update question text
        binding.textQuestion.text = formatQuestionText(currentQuestion.type)

        // Update options
        updateOptions(currentQuestion, state.selectedAnswer)

        // Update button text
        val isLastQuestion = state.currentIndex == state.questions.size - 1
        binding.buttonNext.text = if (isLastQuestion) "Submit" else "Next"
        binding.buttonNext.isEnabled = state.selectedAnswer != null
    }

    private fun formatQuestionText(questionType: QuestionType): String {
        return when (questionType) {
            QuestionType.RELEASE_YEAR -> "What year was this movie released?"
            QuestionType.DIRECTOR -> "Who directed this movie?"
            QuestionType.MAIN_ACTOR -> "Who is the main actor in this movie?"
            QuestionType.RUNTIME -> "What is the runtime of this movie (in minutes)?"
            QuestionType.GENRE -> "What genre is this movie?"
        }
    }

    private fun updateOptions(question: com.example.cinephile.domain.repository.QuizQuestionUiModel, selectedAnswer: String?) {
        // Clear existing buttons
        optionButtons.forEach { binding.containerOptions.removeView(it) }
        optionButtons.clear()

        // Create new buttons for each option
        question.options.forEach { option ->
            val button = MaterialButton(requireContext()).apply {
                text = option
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.MarginLayoutParams.MATCH_PARENT,
                    ViewGroup.MarginLayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 8.dpToPx()
                }
                isCheckable = true
                isChecked = option == selectedAnswer
                setOnClickListener {
                    viewModel.selectAnswer(option)
                    // Update all buttons to reflect selection
                    optionButtons.forEach { it.isChecked = it.text == option }
                }
            }
            optionButtons.add(button)
            binding.containerOptions.addView(button)
        }
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    private fun showSummaryDialog() {
        val state = viewModel.state.value
        val totalQuestions = state.correctCount + state.wrongCount
        
        // Calculate score using the same logic as ViewModel
        val score = if (state.answerHistory.isNotEmpty()) {
            calculateScore(state.answerHistory, state.quizMode, state.quizDifficulty)
        } else {
            0
        }

        val dialog = QuizSummaryDialogFragment.newInstance(
            score = score,
            correctCount = state.correctCount,
            totalQuestions = totalQuestions,
            quizId = state.quizId
        )
        dialog.show(parentFragmentManager, "QuizSummary")
    }

    private fun calculateScore(
        answerHistory: List<com.example.cinephile.ui.quiz.QuestionAnswer>,
        mode: com.example.cinephile.domain.repository.QuizMode,
        difficulty: com.example.cinephile.domain.repository.QuizDifficulty
    ): Int {
        return when (mode) {
            com.example.cinephile.domain.repository.QuizMode.TIMED -> {
                val difficultyMultiplier = when (difficulty) {
                    com.example.cinephile.domain.repository.QuizDifficulty.EASY -> 1.0
                    com.example.cinephile.domain.repository.QuizDifficulty.MEDIUM -> 1.3
                    com.example.cinephile.domain.repository.QuizDifficulty.HARD -> 1.6
                }
                answerHistory.sumOf { answer ->
                    if (answer.isCorrect) {
                        val timeRatio = answer.timeLeft.toDouble() / answer.timeLimit.toDouble()
                        (1000.0 * timeRatio * difficultyMultiplier).toInt()
                    } else {
                        0
                    }
                }
            }
            com.example.cinephile.domain.repository.QuizMode.SURVIVAL -> {
                var score = 0
                var streak = 0
                answerHistory.forEach { answer ->
                    if (answer.isCorrect) {
                        streak++
                        score += 200
                        score += streak * 50
                    } else {
                        streak = 0
                    }
                }
                score
            }
        }
    }
}
