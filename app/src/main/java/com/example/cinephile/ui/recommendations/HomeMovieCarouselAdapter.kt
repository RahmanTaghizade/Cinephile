package com.example.cinephile.ui.recommendations

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.cinephile.R
import com.example.cinephile.databinding.ItemMovieCarouselBinding
import com.example.cinephile.ui.search.MovieUiModel

class HomeMovieCarouselAdapter(
    private val onItemClick: (Long) -> Unit
) : ListAdapter<MovieUiModel, HomeMovieCarouselAdapter.CarouselViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        val binding = ItemMovieCarouselBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CarouselViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CarouselViewHolder(
        private val binding: ItemMovieCarouselBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: MovieUiModel) {
            binding.imagePoster.load(movie.posterUrl) {
                placeholder(R.drawable.ic_launcher_foreground)
                error(R.drawable.ic_launcher_foreground)
                crossfade(true)
            }
            binding.textTitle.text = movie.title
            binding.root.setOnClickListener { onItemClick(movie.id) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<MovieUiModel>() {
        override fun areItemsTheSame(oldItem: MovieUiModel, newItem: MovieUiModel): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MovieUiModel, newItem: MovieUiModel): Boolean =
            oldItem == newItem
    }
}


