package com.example.cinephile.ui.watchlists

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cinephile.R
import com.example.cinephile.domain.repository.WatchlistUiModel

class WatchlistsAdapter(
    private val onItemClick: (WatchlistUiModel) -> Unit,
    private val onRename: (WatchlistUiModel) -> Unit,
    private val onDelete: (WatchlistUiModel) -> Unit
) : ListAdapter<WatchlistUiModel, WatchlistsAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_watchlist, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.text_title)
        private val subtitle: TextView = itemView.findViewById(R.id.text_subtitle)
        private val overflow: ImageButton = itemView.findViewById(R.id.button_overflow)

        fun bind(item: WatchlistUiModel) {
            title.text = item.name
            subtitle.text = if (item.isCurrent) itemView.context.getString(R.string.current_watchlist) else ""

            itemView.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_watchlist_item)
            itemView.isSelected = item.isCurrent

            itemView.setOnClickListener { onItemClick(item) }

            overflow.setOnClickListener { v ->
                // Create a themed context for the popup menu
                val themedContext = ContextThemeWrapper(v.context, R.style.Theme_Cinephile)
                val popup = PopupMenu(themedContext, v)
                MenuInflater(themedContext).inflate(R.menu.menu_watchlist_item, popup.menu)
                
                // Set text color for menu items to white
                for (i in 0 until popup.menu.size()) {
                    val menuItem = popup.menu.getItem(i)
                    val title = menuItem.title
                    if (title != null) {
                        val spannableTitle = android.text.SpannableString(title)
                        spannableTitle.setSpan(
                            android.text.style.ForegroundColorSpan(
                                ContextCompat.getColor(v.context, R.color.white)
                            ),
                            0,
                            title.length,
                            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        menuItem.title = spannableTitle
                    }
                }
                
                // Style the popup menu background using reflection
                try {
                    val popupMenuHelper = PopupMenu::class.java.getDeclaredField("mPopup")
                    popupMenuHelper.isAccessible = true
                    val menuPopupHelper = popupMenuHelper.get(popup)
                    val listView = menuPopupHelper?.javaClass?.getDeclaredMethod("getListView")?.invoke(menuPopupHelper) as? android.widget.ListView
                    listView?.setBackgroundColor(ContextCompat.getColor(v.context, R.color.md_theme_dark_surface))
                } catch (e: Exception) {
                    // Ignore if reflection fails
                }
                
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_rename -> onRename(item)
                        R.id.action_delete -> onDelete(item)
                    }
                    true
                }
                popup.show()
            }
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


