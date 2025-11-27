package com.example.cinephile.ui.quiz

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cinephile.databinding.ItemQuizResultBinding
import com.example.cinephile.domain.repository.QuizResultUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuizResultsAdapter : ListAdapter<QuizResultUiModel, QuizResultsAdapter.ResultViewHolder>(ResultDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val binding = ItemQuizResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ResultViewHolder(
        private val binding: ItemQuizResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(result: QuizResultUiModel) {
            
            binding.textScore.text = "${result.xpEarned} XP"

            
            val date = Date(result.playedAt)
            val dateString = dateFormat.format(date)
            val timeString = timeFormat.format(date)
            
            
            val minutes = result.durationSec / 60
            val seconds = result.durationSec % 60
            val durationString = String.format("%d:%02d", minutes, seconds)
            
            
            binding.textDetails.text = "$dateString at $timeString • $durationString"
            
            val totalQuestions = result.correctCount + result.wrongCount
            val percentage = if (totalQuestions > 0) {
                (result.correctCount * 100) / totalQuestions
            } else {
                0
            }
            binding.textStats.text = "${result.correctCount}/$totalQuestions correct ($percentage%)"
        }
    }

    private class ResultDiffCallback : DiffUtil.ItemCallback<QuizResultUiModel>() {
        override fun areItemsTheSame(oldItem: QuizResultUiModel, newItem: QuizResultUiModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: QuizResultUiModel, newItem: QuizResultUiModel): Boolean {
            return oldItem == newItem
        }
    }
}

