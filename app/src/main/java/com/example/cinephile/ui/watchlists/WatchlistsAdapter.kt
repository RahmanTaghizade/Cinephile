package com.example.cinephile.ui.watchlists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cinephile.databinding.ItemWatchlistWithMoviesBinding
import com.example.cinephile.domain.repository.WatchlistRepository
import com.example.cinephile.domain.repository.WatchlistUiModel
import com.example.cinephile.ui.details.SimilarMoviesAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WatchlistsAdapter(
    private val watchlistRepository: WatchlistRepository,
    private val onWatchlistClick: (WatchlistUiModel) -> Unit,
    private val onMovieClick: (Long) -> Unit
) : ListAdapter<WatchlistUiModel, WatchlistsAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemWatchlistWithMoviesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        holder.cancelJob()
    }

    inner class VH(
        private val binding: ItemWatchlistWithMoviesBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val viewHolderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private var loadJob: Job? = null

        private val moviesAdapter = SimilarMoviesAdapter { movieId ->
            onMovieClick(movieId)
        }

        init {
            binding.recyclerMovies.apply {
                layoutManager = LinearLayoutManager(
                    binding.root.context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                adapter = moviesAdapter
                
                addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: android.graphics.Rect,
                        view: android.view.View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        val position = parent.getChildLayoutPosition(view)
                        if (position == 0) {
                            
                            outRect.left = (16 * binding.root.context.resources.displayMetrics.density).toInt()
                        }
                    }
                })
            }
        }

        fun bind(item: WatchlistUiModel) {
            binding.textTitle.text = item.name

            
            binding.chipCurrent.visibility = if (item.isCurrent) View.VISIBLE else View.GONE

            
            binding.layoutTitle.setOnClickListener {
                onWatchlistClick(item)
            }

            
            cancelJob()

            
            loadJob = viewHolderScope.launch {
                try {
                    watchlistRepository.getWatchlistMovies(item.id).collectLatest { movies ->
                        moviesAdapter.submitList(movies)
                    }
                } catch (e: Exception) {
                    
                    moviesAdapter.submitList(emptyList())
                }
            }
        }

        fun cancelJob() {
            loadJob?.cancel()
            loadJob = null
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WatchlistUiModel>() {
            override fun areItemsTheSame(oldItem: WatchlistUiModel, newItem: WatchlistUiModel): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: WatchlistUiModel, newItem: WatchlistUiModel): Boolean =
                oldItem == newItem
        }
    }
}
