package com.example.cinephile.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.cinephile.R
import com.example.cinephile.databinding.FragmentQuizPlayBinding
import coil.load
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
                            else -> {  }
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

        
        binding.textQuestionNumber.text = "Q ${state.currentIndex + 1}/${state.questions.size}"
        
        binding.textTimer.text = "${state.timeRemaining}s"
        binding.textTimer.setTextColor(
            when {
                state.timeRemaining <= 5 -> ContextCompat.getColor(requireContext(), R.color.md_theme_dark_error)
                state.timeRemaining <= 10 -> ContextCompat.getColor(requireContext(), R.color.md_theme_dark_secondary)
                else -> ContextCompat.getColor(requireContext(), R.color.md_theme_dark_primary)
            }
        )

        
        binding.textQuestion.text = formatQuestionText(currentQuestion.type)
        
        
        updateQuestionImage(currentQuestion, state.submittedAnswer)

        
        updateOptions(currentQuestion, state.selectedAnswer, state.submittedAnswer, state.isAnswerCorrect)

        
        val isLastQuestion = state.currentIndex == state.questions.size - 1
        binding.buttonNext.text = if (isLastQuestion) "Submit" else "Next"
        binding.buttonNext.isEnabled = state.selectedAnswer != null && state.submittedAnswer == null
    }

    private fun formatQuestionText(questionType: QuestionType): String {
        return when (questionType) {
            QuestionType.MOVIE_FROM_DESCRIPTION -> {
                
                "Based on the description, select the correct movie:"
            }
            QuestionType.ACTOR_IN_MOVIE -> "Which actor played in this movie?"
            QuestionType.RELEASE_YEAR -> "When was this movie released?"
            QuestionType.MOVIE_FROM_IMAGE -> "What is the title of this movie?"
        }
    }
    
    private fun updateQuestionImage(question: com.example.cinephile.domain.repository.QuizQuestionUiModel, submittedAnswer: String?) {
        
        val shouldShowImage = when (question.type) {
            QuestionType.MOVIE_FROM_DESCRIPTION -> {
                
                submittedAnswer != null && question.moviePosterUrl != null
            }
            QuestionType.MOVIE_FROM_IMAGE -> question.moviePosterUrl != null
            QuestionType.ACTOR_IN_MOVIE -> question.moviePosterUrl != null
            QuestionType.RELEASE_YEAR -> question.moviePosterUrl != null
        }
        
        if (shouldShowImage && question.moviePosterUrl != null) {
            binding.imageMoviePoster.visibility = View.VISIBLE
            binding.imageMoviePoster.load(question.moviePosterUrl) {
                placeholder(R.drawable.ic_placeholder_movie)
                error(R.drawable.ic_placeholder_movie)
                fallback(R.drawable.ic_placeholder_movie)
                crossfade(true)
            }
        } else {
            binding.imageMoviePoster.visibility = View.GONE
        }
        
        
        val showDescription = question.type == QuestionType.MOVIE_FROM_DESCRIPTION && question.movieDescription != null
        binding.textDescription.visibility = if (showDescription) View.VISIBLE else View.GONE
        if (showDescription) {
            binding.textDescription.text = question.movieDescription
        }
    }

    private fun updateOptions(
        question: com.example.cinephile.domain.repository.QuizQuestionUiModel,
        selectedAnswer: String?,
        submittedAnswer: String?,
        isAnswerCorrect: Boolean?
    ) {
        
        optionButtons.forEach { binding.containerOptions.removeView(it) }
        optionButtons.clear()

        val correctAnswer = question.correctAnswer
        val isAnswerSubmitted = submittedAnswer != null

        
        question.options.forEach { option ->
            val button = MaterialButton(requireContext()).apply {
                text = option
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.MarginLayoutParams.MATCH_PARENT,
                    ViewGroup.MarginLayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 8.dpToPx()
                }
                
                
                when {
                    
                    isAnswerSubmitted -> {
                        when {
                            option == correctAnswer -> {
                                
                                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_theme_dark_successContainer))
                                setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_dark_onSuccessContainer))
                                isEnabled = false
                            }
                            option == submittedAnswer && !isAnswerCorrect!! -> {
                                
                                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_theme_dark_errorContainer))
                                setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_dark_onErrorContainer))
                                isEnabled = false
                            }
                            else -> {
                                
                                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_theme_dark_surfaceVariant))
                                setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_dark_onSurfaceVariant))
                                isEnabled = false
                            }
                        }
                    }
                    
                    option == selectedAnswer -> {
                        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_theme_dark_primaryContainer))
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_dark_onPrimaryContainer))
                        isEnabled = true
                    }
                    
                    else -> {
                        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_theme_dark_surfaceVariant))
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_dark_onSurfaceVariant))
                        isEnabled = true
                    }
                }
                
                setOnClickListener {
                    if (!isAnswerSubmitted) {
                        viewModel.selectAnswer(option)
                    }
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
        val baseXP = state.correctCount * 10
        val timeBonus = calculateTimeBonus(state.totalTimeSpent, state.correctCount)
        val xpEarned = baseXP + timeBonus
        val duration = ((System.currentTimeMillis() - state.startTime) / 1000).toInt()
        val percentage = if (totalQuestions > 0) {
            (state.correctCount * 100) / totalQuestions
        } else {
            0
        }

        val dialog = QuizSummaryDialogFragment.newInstance(
            xpEarned = xpEarned,
            durationSec = duration,
            correctCount = state.correctCount,
            totalQuestions = totalQuestions,
            percentage = percentage,
            quizId = state.quizId
        )
        dialog.show(parentFragmentManager, "QuizSummary")
    }

    private fun calculateTimeBonus(totalTimeMs: Long, correctCount: Int): Int {
        if (correctCount == 0) return 0
        val avgTimeSeconds = (totalTimeMs / correctCount) / 1000
        
        return when {
            avgTimeSeconds <= 5 -> correctCount * 5
            avgTimeSeconds <= 10 -> correctCount * 3
            avgTimeSeconds <= 15 -> correctCount * 2
            else -> 0
        }
    }
}
