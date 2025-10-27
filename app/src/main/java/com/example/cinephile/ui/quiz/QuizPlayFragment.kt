package com.example.cinephile.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.cinephile.databinding.FragmentQuizPlayBinding

class QuizPlayFragment : Fragment() {
    private var _binding: FragmentQuizPlayBinding? = null
    private val binding get() = _binding!!

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
        binding.textQuizPlay.text = "Quiz Play Fragment"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
