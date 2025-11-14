package com.example.cinephile.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.cinephile.databinding.ItemSearchMovieBinding
import com.example.cinephile.databinding.ItemSearchPersonBinding
import com.example.cinephile.data.remote.TmdbPerson

class SearchResultsAdapter(
    private val onMovieClick: (Long) -> Unit,
    private val onPersonClick: (TmdbPerson) -> Unit,
    private val onSeriesClick: ((Long) -> Unit)? = null
) : ListAdapter<SearchResult, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is SearchResult.MovieItem -> 0
        is SearchResult.PersonItem -> 1
        is SearchResult.SeriesItem -> 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> MovieVH(ItemSearchMovieBinding.inflate(inflater, parent, false), onMovieClick)
            2 -> SeriesVH(ItemSearchMovieBinding.inflate(inflater, parent, false), onSeriesClick)
            else -> PersonVH(ItemSearchPersonBinding.inflate(inflater, parent, false), onPersonClick)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MovieVH -> holder.bind((getItem(position) as SearchResult.MovieItem).movie)
            is SeriesVH -> holder.bind((getItem(position) as SearchResult.SeriesItem).series)
            is PersonVH -> holder.bind((getItem(position) as SearchResult.PersonItem).person)
        }
    }

    class MovieVH(
        private val binding: ItemSearchMovieBinding,
        private val onClick: (Long) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: MovieUiModel) {
            binding.imagePoster.load(movie.posterUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
            }
            binding.textTitle.text = movie.title
            // Show only genres, no year
            val subtitle = movie.genres.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: ""
            binding.textSubtitle.text = subtitle
            val rating = movie.voteAverage
            if (rating > 0) {
                binding.textBadge.text = String.format("%.1f", rating)
                binding.textBadge.visibility = android.view.View.VISIBLE
            } else binding.textBadge.visibility = android.view.View.GONE
            
            // Ensure movie images are square (not circular)
            binding.imagePoster.clipToOutline = false
            binding.imagePoster.outlineProvider = null
            
            binding.root.setOnClickListener { onClick(movie.id) }
        }
    }

    class SeriesVH(
        private val binding: ItemSearchMovieBinding,
        private val onClick: ((Long) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(series: TvSeriesUiModel) {
            binding.imagePoster.load(series.posterUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
            }
            binding.textTitle.text = series.name
            // Show only genres, no year
            val subtitle = series.genres.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: ""
            binding.textSubtitle.text = subtitle
            val rating = series.voteAverage
            if (rating > 0) {
                binding.textBadge.text = String.format("%.1f", rating)
                binding.textBadge.visibility = android.view.View.VISIBLE
            } else binding.textBadge.visibility = android.view.View.GONE

            binding.imagePoster.clipToOutline = false
            binding.imagePoster.outlineProvider = null

            binding.root.setOnClickListener { onClick?.invoke(series.id) }
        }
    }

    class PersonVH(
        private val binding: ItemSearchPersonBinding,
        private val onClick: (TmdbPerson) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(person: com.example.cinephile.data.remote.TmdbPerson) {
            binding.textName.text = person.name
            // Format the department nicely - capitalize first letter
            val department = person.knownForDepartment.takeIf { it.isNotBlank() } 
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                ?: ""
            binding.textRole.text = department
            
            // Load profile image
            val profileImageUrl = person.profilePath?.let { 
                "https://image.tmdb.org/t/p/w185$it" 
            }
            binding.imageAvatar.load(profileImageUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
            }
            
            // Make image circular
            binding.imageAvatar.post {
                binding.imageAvatar.outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
                binding.imageAvatar.clipToOutline = true
            }
            
            binding.root.setOnClickListener { onClick(person) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<SearchResult>() {
        override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean =
            when {
                oldItem is SearchResult.MovieItem && newItem is SearchResult.MovieItem -> oldItem.movie.id == newItem.movie.id
                oldItem is SearchResult.SeriesItem && newItem is SearchResult.SeriesItem -> oldItem.series.id == newItem.series.id
                oldItem is SearchResult.PersonItem && newItem is SearchResult.PersonItem -> oldItem.person.id == newItem.person.id
                else -> false
            }

        override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean = oldItem == newItem
    }
}


