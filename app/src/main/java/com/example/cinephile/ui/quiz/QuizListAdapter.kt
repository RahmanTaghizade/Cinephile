package com.example.cinephile.ui.quiz

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cinephile.databinding.ItemQuizBinding
import java.text.SimpleDateFormat
import java.util.Locale

class QuizListAdapter(
    private val onItemClick: (Long) -> Unit
) : ListAdapter<QuizListItem, QuizListAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuizBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemQuizBinding,
        private val onItemClick: (Long) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        fun bind(item: QuizListItem) {
            binding.textTitle.text = item.name
            
            val subtitle = buildString {
                append(item.watchlistName)
                append(" • ")
                append("${item.questionCount} questions")
                append(" • ")
                append(item.difficulty.name)
                append(" • ")
                append(item.mode.name)
            }
            binding.textSubtitle.text = subtitle
            
            binding.textBadge.text = dateFormat.format(item.createdAt)
            
            binding.root.setOnClickListener {
                onItemClick(item.id)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<QuizListItem>() {
            override fun areItemsTheSame(oldItem: QuizListItem, newItem: QuizListItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: QuizListItem, newItem: QuizListItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}


