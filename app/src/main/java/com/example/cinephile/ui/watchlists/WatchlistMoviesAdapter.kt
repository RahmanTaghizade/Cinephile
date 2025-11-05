package com.example.cinephile.ui.watchlists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cinephile.R
import com.example.cinephile.ui.search.MovieUiModel

class WatchlistMoviesAdapter(
    private val onClick: (MovieUiModel) -> Unit,
    private val onDelete: (MovieUiModel) -> Unit
) : ListAdapter<MovieUiModel, WatchlistMoviesAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_watchlist_movie, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val poster: ImageView = itemView.findViewById(R.id.image_poster)
        private val title: TextView = itemView.findViewById(R.id.text_title)
        private val subtitle: TextView = itemView.findViewById(R.id.text_subtitle)
        private val delete: ImageButton = itemView.findViewById(R.id.button_delete)

        fun bind(item: MovieUiModel) {
            title.text = item.title
            subtitle.text = item.releaseDate ?: ""
            // For now, use a placeholder; image loading lib not wired here
            poster.setImageResource(android.R.drawable.ic_menu_report_image)
            itemView.setOnClickListener { onClick(item) }
            delete.setOnClickListener { onDelete(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MovieUiModel>() {
            override fun areItemsTheSame(oldItem: MovieUiModel, newItem: MovieUiModel): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MovieUiModel, newItem: MovieUiModel): Boolean =
                oldItem == newItem
        }
    }
}


