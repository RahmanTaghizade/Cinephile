package com.example.cinephile.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cinephile.R
import com.example.cinephile.databinding.FragmentQuizListBinding
import com.example.cinephile.domain.repository.QuizDifficulty
import com.example.cinephile.domain.repository.QuizMode
import com.example.cinephile.ui.quiz.QuizListFragmentDirections
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuizListFragment : Fragment() {
    private var _binding: FragmentQuizListBinding? = null
    private val binding get() = _binding!!
    
    val viewModel: QuizListViewModel by viewModels()
    private lateinit var adapter: QuizListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = QuizListAdapter { quizId ->
            val action = QuizListFragmentDirections.actionQuizListFragmentToQuizDetailsFragment(quizId)
            findNavController().navigate(action)
        }
        
        binding.recyclerQuizzes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@QuizListFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.quizzes.collect { quizzes ->
                adapter.submitList(quizzes)
                binding.textEmpty.visibility = if (quizzes.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupFab() {
        binding.fabCreate.setOnClickListener {
            showCreateQuizDialog()
        }
    }

    private fun showCreateQuizDialog() {
        val dialog = CreateQuizDialogFragment().apply {
            onCreateQuiz = { name, watchlistId, questionCount, difficulty, mode ->
                viewModel.createQuiz(name, watchlistId, questionCount, difficulty, mode)
            }
            onQuizCreated = { quizId ->
                val action = QuizListFragmentDirections.actionQuizListFragmentToQuizDetailsFragment(quizId)
                findNavController().navigate(action)
            }
        }
        dialog.show(childFragmentManager, "CreateQuizDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


