package com.example.cinephile.ui.actor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.cinephile.R
import com.example.cinephile.databinding.ItemSearchMovieBinding
import com.example.cinephile.domain.repository.PersonCreditType

data class ActorMovieUiModel(
    val id: Long,
    val title: String,
    val posterUrl: String?,
    val releaseYear: String?,
    val role: String?,
    val type: PersonCreditType,
    val voteAverage: Double
)

class ActorMoviesAdapter(
    private val onMovieClick: (Long) -> Unit
) : ListAdapter<ActorMovieUiModel, ActorMoviesAdapter.ActorMovieViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActorMovieViewHolder {
        val binding = ItemSearchMovieBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ActorMovieViewHolder(binding, onMovieClick)
    }

    override fun onBindViewHolder(holder: ActorMovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ActorMovieViewHolder(
        private val binding: ItemSearchMovieBinding,
        private val onMovieClick: (Long) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ActorMovieUiModel) {
            binding.textTitle.text = item.title
            val roleText = when {
                item.role.isNullOrBlank() -> null
                item.type == PersonCreditType.CAST ->
                    binding.root.context.getString(R.string.actor_role_character, item.role)
                else -> item.role
            }
            val subtitle = listOfNotNull(item.releaseYear, roleText).joinToString(" • ")
            binding.textSubtitle.text = subtitle

            binding.textBadge.isVisible = item.voteAverage > 0
            if (item.voteAverage > 0) {
                binding.textBadge.text = String.format("%.1f", item.voteAverage)
            }

            binding.imagePoster.load(item.posterUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
            }
            binding.imagePoster.clipToOutline = false
            binding.imagePoster.outlineProvider = null

            binding.root.setOnClickListener { onMovieClick(item.id) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ActorMovieUiModel>() {
        override fun areItemsTheSame(oldItem: ActorMovieUiModel, newItem: ActorMovieUiModel): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ActorMovieUiModel, newItem: ActorMovieUiModel): Boolean =
            oldItem == newItem
    }
}

