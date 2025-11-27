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
    private val onItemClick: (MovieUiModel) -> Unit = { movie -> },
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
                
                val posterUrl = movie.posterUrl?.takeIf { it.isNotBlank() }
                imageMoviePoster.load(posterUrl) {
                    placeholder(R.drawable.ic_placeholder_movie)
                    error(R.drawable.ic_placeholder_movie)
                    fallback(R.drawable.ic_placeholder_movie)
                    crossfade(true)
                }

                
                textMovieTitle.text = movie.title
                textMovieDirector.text = movie.director?.let {
                    root.context.getString(R.string.movie_director_prefix, it)
                } ?: ""
                textMovieDate.text = movie.releaseDate?.let {
                    root.context.getString(R.string.movie_date_prefix, it)
                } ?: ""

                
                if (movie.userRating > 0f) {
                    textRatingBadge.text = root.context.getString(R.string.rating_badge, movie.userRating)
                    textRatingBadge.visibility = android.view.View.VISIBLE
                } else {
                    textRatingBadge.visibility = android.view.View.GONE
                }

                
                root.setOnClickListener {
                    onItemClick(movie)
                }
                root.setOnLongClickListener {
                    
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
