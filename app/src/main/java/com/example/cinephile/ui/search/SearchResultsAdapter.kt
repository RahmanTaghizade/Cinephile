package com.example.cinephile.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.cinephile.R
import com.example.cinephile.databinding.ItemSearchMovieBinding
import com.example.cinephile.databinding.ItemSearchPersonBinding
import com.example.cinephile.data.remote.TmdbPerson

class SearchResultsAdapter(
    private val onMovieClick: (Long) -> Unit,
    private val onPersonClick: (TmdbPerson) -> Unit,
    private val onSeriesClick: ((Long) -> Unit)? = null,
    private val onMovieLongPress: ((Long) -> Unit)? = null,
    private val onSeriesLongPress: ((Long) -> Unit)? = null
) : ListAdapter<SearchResult, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is SearchResult.MovieItem -> 0
        is SearchResult.PersonItem -> 1
        is SearchResult.SeriesItem -> 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> MovieVH(ItemSearchMovieBinding.inflate(inflater, parent, false), onMovieClick, onMovieLongPress)
            2 -> SeriesVH(ItemSearchMovieBinding.inflate(inflater, parent, false), onSeriesClick, onSeriesLongPress)
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
        private val onClick: (Long) -> Unit,
        private val onLongPress: ((Long) -> Unit)? = null
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: MovieUiModel) {
            val posterUrl = movie.posterUrl?.takeIf { it.isNotBlank() }
            binding.imagePoster.load(posterUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder_movie)
                error(R.drawable.ic_placeholder_movie)
                fallback(R.drawable.ic_placeholder_movie)
            }
            
            binding.imagePoster.post {
                val radius = 12 * binding.root.context.resources.displayMetrics.density
                binding.imagePoster.outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radius)
                    }
                }
                binding.imagePoster.clipToOutline = true
            }
            binding.textTitle.text = movie.title
            
            val genres = movie.genres.take(2)
            val subtitle = if (genres.isNotEmpty()) {
                genres.joinToString(", ")
            } else {
                ""
            }
            binding.textSubtitle.text = subtitle
            
            val rating = movie.voteAverage
            if (rating > 0) {
                binding.textBadge.text = String.format("%.1f", rating)
                binding.textBadge.visibility = android.view.View.VISIBLE
                
                
                val badgeDrawable = when {
                    rating <= 4.0 -> R.drawable.rating_badge_red
                    rating > 4.0 && rating <= 7.0 -> R.drawable.rating_badge_yellow
                    else -> R.drawable.rating_badge_bg 
                }
                binding.textBadge.setBackgroundResource(badgeDrawable)
            } else {
                binding.textBadge.visibility = android.view.View.GONE
            }
            
            binding.root.setOnClickListener { onClick(movie.id) }
            binding.root.setOnLongClickListener {
                onLongPress?.invoke(movie.id)
                true
            }
        }
    }

    class SeriesVH(
        private val binding: ItemSearchMovieBinding,
        private val onClick: ((Long) -> Unit)?,
        private val onLongPress: ((Long) -> Unit)? = null
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(series: TvSeriesUiModel) {
            val posterUrl = series.posterUrl?.takeIf { it.isNotBlank() }
            binding.imagePoster.load(posterUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder_movie)
                error(R.drawable.ic_placeholder_movie)
                fallback(R.drawable.ic_placeholder_movie)
            }
            
            binding.imagePoster.post {
                val radius = 12 * binding.root.context.resources.displayMetrics.density
                binding.imagePoster.outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radius)
                    }
                }
                binding.imagePoster.clipToOutline = true
            }
            binding.textTitle.text = series.name
            
            val genres = series.genres.take(2)
            val subtitle = if (genres.isNotEmpty()) {
                genres.joinToString(", ")
            } else {
                ""
            }
            binding.textSubtitle.text = subtitle
            
            val rating = series.voteAverage
            if (rating > 0) {
                binding.textBadge.text = String.format("%.1f", rating)
                binding.textBadge.visibility = android.view.View.VISIBLE
                
                
                val badgeDrawable = when {
                    rating <= 4.0 -> R.drawable.rating_badge_red
                    rating > 4.0 && rating <= 7.0 -> R.drawable.rating_badge_yellow
                    else -> R.drawable.rating_badge_bg 
                }
                binding.textBadge.setBackgroundResource(badgeDrawable)
            } else {
                binding.textBadge.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onClick?.invoke(series.id) }
            binding.root.setOnLongClickListener {
                onLongPress?.invoke(series.id)
                true
            }
        }
    }

    class PersonVH(
        private val binding: ItemSearchPersonBinding,
        private val onClick: (TmdbPerson) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(person: com.example.cinephile.data.remote.TmdbPerson) {
            binding.textName.text = person.name
            
            val department = person.knownForDepartment.takeIf { it.isNotBlank() } 
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                ?: ""
            binding.textRole.text = department
            
            
            val profileImageUrl = person.profilePath?.takeIf { it.isNotBlank() }?.let { 
                "https://image.tmdb.org/t/p/w185$it" 
            }
            binding.imageAvatar.load(profileImageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder_person)
                error(R.drawable.ic_placeholder_person)
                fallback(R.drawable.ic_placeholder_person)
            }
            
            
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


