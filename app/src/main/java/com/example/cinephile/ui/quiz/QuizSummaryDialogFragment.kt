package com.example.cinephile.ui.quiz

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.NavHostFragment
import com.example.cinephile.R
import com.example.cinephile.databinding.DialogQuizSummaryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class QuizSummaryDialogFragment : DialogFragment() {
    private var _binding: DialogQuizSummaryBinding? = null
    private val binding get() = _binding!!

    private var score: Int = 0
    private var correctCount: Int = 0
    private var totalQuestions: Int = 0
    private var quizId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            score = it.getInt(ARG_SCORE, 0)
            correctCount = it.getInt(ARG_CORRECT_COUNT, 0)
            totalQuestions = it.getInt(ARG_TOTAL_QUESTIONS, 0)
            quizId = it.getLong(ARG_QUIZ_ID, 0L)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogQuizSummaryBinding.inflate(layoutInflater)

        binding.textScore.text = "Score: $score"
        binding.textDetails.text = "$correctCount out of $totalQuestions correct"

        binding.buttonDone.setOnClickListener {
            dismiss()
            navigateBackToDetails()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun navigateBackToDetails() {
        // Get the nav controller from the parent fragment (QuizPlayFragment)
        val navController = parentFragment?.let {
            NavHostFragment.findNavController(it)
        } ?: return

        // Pop back to quiz details (this will pop QuizPlayFragment and return to QuizDetailsFragment)
        if (navController.currentDestination?.id == R.id.quizPlayFragment) {
            navController.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SCORE = "score"
        private const val ARG_CORRECT_COUNT = "correct_count"
        private const val ARG_TOTAL_QUESTIONS = "total_questions"
        private const val ARG_QUIZ_ID = "quiz_id"

        fun newInstance(score: Int, correctCount: Int, totalQuestions: Int, quizId: Long = 0L): QuizSummaryDialogFragment {
            return QuizSummaryDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SCORE, score)
                    putInt(ARG_CORRECT_COUNT, correctCount)
                    putInt(ARG_TOTAL_QUESTIONS, totalQuestions)
                    putLong(ARG_QUIZ_ID, quizId)
                }
            }
        }
    }
}

