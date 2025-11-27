package com.example.cinephile.ui.quiz

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cinephile.R
import com.example.cinephile.databinding.DialogQuizSummaryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class QuizSummaryDialogFragment : DialogFragment() {
    private var _binding: DialogQuizSummaryBinding? = null
    private val binding get() = _binding!!

    private var xpEarned: Int = 0
    private var durationSec: Int = 0
    private var correctCount: Int = 0
    private var totalQuestions: Int = 0
    private var percentage: Int = 0
    private var quizId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            xpEarned = it.getInt(ARG_XP_EARNED, 0)
            durationSec = it.getInt(ARG_DURATION_SEC, 0)
            correctCount = it.getInt(ARG_CORRECT_COUNT, 0)
            totalQuestions = it.getInt(ARG_TOTAL_QUESTIONS, 0)
            percentage = it.getInt(ARG_PERCENTAGE, 0)
            quizId = it.getLong(ARG_QUIZ_ID, 0L)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogQuizSummaryBinding.inflate(layoutInflater)

        binding.textScore.text = "$xpEarned XP Earned"
        
        val minutes = durationSec / 60
        val seconds = durationSec % 60
        val timeText = if (minutes > 0) {
            "${minutes}m ${seconds}s"
        } else {
            "${seconds}s"
        }
        binding.textDetails.text = "$correctCount out of $totalQuestions correct ($percentage%)"
        binding.textTime.text = "Time: $timeText"

        binding.buttonDone.setOnClickListener {
            dismiss()
            
            navigateBackToDetails()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun navigateBackToDetails() {
        
        
        
        
        
        val parent = parentFragment
        if (parent is Fragment) {
            try {
                
                val navController = parent.findNavController()
                
                navController.popBackStack(R.id.quizListFragment, false)
                return
            } catch (e: Exception) {
                
            }
        }
        
        
        try {
            val navHostFragment = requireActivity().supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? androidx.navigation.fragment.NavHostFragment
            navHostFragment?.navController?.let { navController ->
                
                navController.popBackStack(R.id.quizListFragment, false)
            }
        } catch (e: Exception) {
            
            try {
                val navHostFragment = requireActivity().supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as? androidx.navigation.fragment.NavHostFragment
                val navController = navHostFragment?.navController ?: return
                
                if (navController.currentDestination?.id == R.id.quizPlayFragment) {
                    navController.popBackStack() 
                }
                if (navController.currentDestination?.id == R.id.quizDetailsFragment) {
                    navController.popBackStack() 
                }
            } catch (e2: Exception) {
                
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_XP_EARNED = "xp_earned"
        private const val ARG_DURATION_SEC = "duration_sec"
        private const val ARG_CORRECT_COUNT = "correct_count"
        private const val ARG_TOTAL_QUESTIONS = "total_questions"
        private const val ARG_PERCENTAGE = "percentage"
        private const val ARG_QUIZ_ID = "quiz_id"

        fun newInstance(
            xpEarned: Int,
            durationSec: Int,
            correctCount: Int,
            totalQuestions: Int,
            percentage: Int,
            quizId: Long = 0L
        ): QuizSummaryDialogFragment {
            return QuizSummaryDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_XP_EARNED, xpEarned)
                    putInt(ARG_DURATION_SEC, durationSec)
                    putInt(ARG_CORRECT_COUNT, correctCount)
                    putInt(ARG_TOTAL_QUESTIONS, totalQuestions)
                    putInt(ARG_PERCENTAGE, percentage)
                    putLong(ARG_QUIZ_ID, quizId)
                }
            }
        }
    }
}

