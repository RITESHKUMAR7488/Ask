package com.example.ask.search.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ask.R
import com.example.ask.databinding.ItemSearchResultBinding
import com.example.ask.search.model.SearchResult
import com.example.ask.search.model.SearchResultType

class SearchResultAdapter(
    private val context: Context,
    private val onResultClick: (SearchResult) -> Unit
) : ListAdapter<SearchResult, SearchResultAdapter.SearchResultViewHolder>(SearchResultDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SearchResultViewHolder(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(result: SearchResult) {
            binding.tvResultTitle.text = result.title
            binding.tvResultSubtitle.text = result.subtitle ?: ""
            binding.tvResultSubtitle.visibility = if (result.subtitle.isNullOrBlank()) ViewGroup.GONE else ViewGroup.VISIBLE

            // Set Type Chip
            binding.chipResultType.text = result.type.name
            val chipColor = when (result.type) {
                SearchResultType.QUERY -> R.color.help_chip_color
                SearchResultType.COMMUNITY -> R.color.community_chip_color
                SearchResultType.USER -> R.color.info_color
            }
            binding.chipResultType.setChipBackgroundColorResource(chipColor)
            binding.chipResultType.setTextColor(ContextCompat.getColor(context, R.color.white))


            // Load Image
            val placeholderIcon = when (result.type) {
                SearchResultType.QUERY -> R.drawable.ic_help
                SearchResultType.COMMUNITY -> R.drawable.ic_group
                SearchResultType.USER -> R.drawable.ic_person
            }

            Glide.with(context)
                .load(result.imageUrl)
                .placeholder(placeholderIcon)
                .error(placeholderIcon)
                .circleCrop()
                .into(binding.ivResultImage)

            binding.root.setOnClickListener { onResultClick(result) }
        }
    }

    class SearchResultDiffCallback : DiffUtil.ItemCallback<SearchResult>() {
        override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem.id == newItem.id && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem == newItem
        }
    }
}