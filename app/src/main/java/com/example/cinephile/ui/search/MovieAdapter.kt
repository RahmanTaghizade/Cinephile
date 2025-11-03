package com.example.cinephile.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.ImageRequest
import com.example.cinephile.R
import com.example.cinephile.databinding.ItemMovieBinding

class MovieAdapter(
    private val onItemClick: (Long) -> Unit = {},
    private val onLongPress: (Long) -> Unit = {}
) : ListAdapter<MovieUiModel, MovieAdapter.MovieViewHolder>(MovieDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MovieViewHolder(
        private val binding: ItemMovieBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: MovieUiModel) {
            with(binding) {
                // Load poster image with Coil
                imageMoviePoster.load(movie.posterUrl) {
                    placeholder(R.drawable.ic_launcher_foreground)
                    error(R.drawable.ic_launcher_foreground)
                    crossfade(true)
                }

                // Set movie details
                textMovieTitle.text = movie.title
                textMovieDirector.text = movie.director?.let {
                    root.context.getString(R.string.movie_director_prefix, it)
                } ?: ""
                textMovieDate.text = movie.releaseDate?.let {
                    root.context.getString(R.string.movie_date_prefix, it)
                } ?: ""

                // Show/hide rating badge
                if (movie.userRating > 0f) {
                    textRatingBadge.text = root.context.getString(R.string.rating_badge, movie.userRating)
                    textRatingBadge.visibility = android.view.View.VISIBLE
                } else {
                    textRatingBadge.visibility = android.view.View.GONE
                }

                // Set heart icon based on favorite status
                imageHeart.setImageResource(
                    if (movie.isFavorite) {
                        android.R.drawable.btn_star_big_on
                    } else {
                        android.R.drawable.btn_star_big_off
                    }
                )

                // Set click listeners
                root.setOnClickListener {
                    onItemClick(movie.id)
                }
                root.setOnLongClickListener {
                    // Long-press callback, passes movieId (Long) to VM
                    onLongPress(movie.id)
                    true
                }
            }
        }
    }

    private class MovieDiffCallback : DiffUtil.ItemCallback<MovieUiModel>() {
        override fun areItemsTheSame(oldItem: MovieUiModel, newItem: MovieUiModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MovieUiModel, newItem: MovieUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
