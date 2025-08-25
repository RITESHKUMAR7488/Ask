package com.example.ask.mainModule.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ask.R
import com.example.ask.addModule.models.QueryModel
import com.example.ask.databinding.ItemQueryBinding
import java.text.SimpleDateFormat
import java.util.*

class QueryAdapter(
    private val context: Context,
    private val onQueryClick: (QueryModel) -> Unit,
    private val onHelpClick: (QueryModel) -> Unit, // ✅ Help button callback
    private val onChatClick: (QueryModel) -> Unit  // ✅ Chat button callback
) : ListAdapter<QueryModel, QueryAdapter.QueryViewHolder>(QueryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueryViewHolder {
        val binding = ItemQueryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QueryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QueryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class QueryViewHolder(private val binding: ItemQueryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(query: QueryModel) {
            with(binding) {
                // Basic query info
                tvTitle.text = query.title
                tvDescription.text = query.description
                tvLocation.text = query.location
                chipCategory.text = query.category
                tvUserName.text = query.userName

                // Community info
                if (!query.communityName.isNullOrEmpty()) {
                    tvCommunityName.text = query.communityName
                    layoutCommunityInfo.visibility = View.VISIBLE
                } else {
                    layoutCommunityInfo.visibility = View.GONE
                }

                // User profile image
                if (!query.userProfileImage.isNullOrEmpty()) {
                    Glide.with(context)
                        .load(query.userProfileImage)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivUserProfile)
                } else {
                    ivUserProfile.setImageResource(R.drawable.ic_person)
                }

                // Query image
                if (!query.imageUrl.isNullOrEmpty()) {
                    ivQueryImage.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(query.imageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(ivQueryImage)
                } else {
                    ivQueryImage.visibility = View.GONE
                }

                // Status
                setStatusUI(query.status ?: "OPEN")

                // Priority
                setPriorityUI(query.priority ?: "NORMAL")

                // Timestamp
                query.timestamp?.let { timestamp ->
                    tvTimestamp.text = formatTimestamp(timestamp)
                }

                // Response count
                tvResponseCount.text = "${query.responseCount} responses"

                // Tags
                if (!query.tags.isNullOrEmpty()) {
                    tvTags.visibility = View.VISIBLE
                    tvTags.text = query.tags?.joinToString(", ") { "#$it" }
                } else {
                    tvTags.visibility = View.GONE
                }

                // ✅ Setup action buttons
                setupActionButtons(query)

                // Click listener for the main item
                root.setOnClickListener {
                    onQueryClick(query)
                }
            }
        }

        // ✅ Setup action buttons (Help and Chat)
        private fun setupActionButtons(query: QueryModel) {
            with(binding) {
                // Show action buttons layout
                layoutActionButtons.visibility = View.VISIBLE

                // Help button click listener
                btnHelp.setOnClickListener {
                    onHelpClick(query)
                }

                // Chat button click listener
                btnChat.setOnClickListener {
                    onChatClick(query)
                }

                // Customize button state based on query status
                when (query.status) {
                    "RESOLVED", "CLOSED" -> {
                        btnHelp.isEnabled = false
                        btnHelp.alpha = 0.5f
                        btnChat.isEnabled = false
                        btnChat.alpha = 0.5f
                    }
                    else -> {
                        btnHelp.isEnabled = true
                        btnHelp.alpha = 1.0f
                        btnChat.isEnabled = true
                        btnChat.alpha = 1.0f
                    }
                }
            }
        }

        private fun setStatusUI(status: String) {
            with(binding) {
                tvStatus.text = status

                // ✅ FIXED: Replaced destructuring assignment with direct assignment
                val colorRes: Int
                val textColorRes: Int

                when (status) {
                    "OPEN" -> {
                        colorRes = R.color.status_open_color
                        textColorRes = R.color.white
                    }
                    "IN_PROGRESS" -> {
                        colorRes = R.color.status_progress_color
                        textColorRes = R.color.white
                    }
                    "RESOLVED" -> {
                        colorRes = R.color.success_color
                        textColorRes = R.color.white
                    }
                    "CLOSED" -> {
                        colorRes = R.color.status_closed_color
                        textColorRes = R.color.white
                    }
                    else -> {
                        colorRes = R.color.status_default
                        textColorRes = R.color.white
                    }
                }

                chipStatus.setChipBackgroundColorResource(colorRes)
                chipStatus.setTextColor(ContextCompat.getColor(context, textColorRes))
            }
        }

        private fun setPriorityUI(priority: String) {
            with(binding) {
                chipPriority.text = priority

                // ✅ FIXED: Direct assignment instead of destructuring
                val colorRes = when (priority) {
                    "HIGH" -> R.color.error_color
                    "NORMAL" -> R.color.primary_color
                    "LOW" -> R.color.gray_500
                    else -> R.color.gray_400
                }

                chipPriority.setChipBackgroundColorResource(colorRes)
                chipPriority.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val difference = now - timestamp

            return when {
                difference < 60_000 -> "Just now"
                difference < 3600_000 -> "${difference / 60_000}m ago"
                difference < 86400_000 -> "${difference / 3600_000}h ago"
                difference < 604800_000 -> "${difference / 86400_000}d ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    class QueryDiffCallback : DiffUtil.ItemCallback<QueryModel>() {
        override fun areItemsTheSame(oldItem: QueryModel, newItem: QueryModel): Boolean {
            return oldItem.queryId == newItem.queryId
        }

        override fun areContentsTheSame(oldItem: QueryModel, newItem: QueryModel): Boolean {
            return oldItem == newItem
        }
    }
}