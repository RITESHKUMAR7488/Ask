package com.example.ask.communityModule.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ask.communityModule.models.CommunityModels
import com.example.ask.databinding.CommunityItemBinding
import java.util.Locale

class MyCommunityAdapter(
    private val onCommunityClick: (CommunityModels) -> Unit
) : ListAdapter<CommunityModels, MyCommunityAdapter.CommunityViewHolder>(CommunityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val binding = CommunityItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommunityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommunityViewHolder(
        private val binding: CommunityItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(community: CommunityModels) = with(binding) {
            tvCommunityName.text = community.communityName
            tvRole.text = community.role?.uppercase(Locale.getDefault())
            tvJoinedDate.text = formatJoinedDate(community.joinedAt)

            // role-based background
            when (community.role?.lowercase(Locale.getDefault())) {
                "admin" -> tvRole.setBackgroundResource(android.R.color.holo_red_light)
                "member" -> tvRole.setBackgroundResource(android.R.color.holo_blue_light)
                else -> tvRole.setBackgroundResource(android.R.color.darker_gray)
            }

            root.setOnClickListener { onCommunityClick(community) }
        }

        private fun formatJoinedDate(timestamp: Long?): String {
            if (timestamp == null) return "Unknown"

            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60_000 -> "Just now"
                diff < 3_600_000 -> "${diff / 60_000} minutes ago"
                diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
                diff < 604_800_000 -> "${diff / 86_400_000} days ago"
                else -> "${diff / 604_800_000} weeks ago"
            }
        }
    }

    private class CommunityDiffCallback : DiffUtil.ItemCallback<CommunityModels>() {
        override fun areItemsTheSame(oldItem: CommunityModels, newItem: CommunityModels): Boolean {
            return oldItem.communityId == newItem.communityId
        }

        override fun areContentsTheSame(oldItem: CommunityModels, newItem: CommunityModels): Boolean {
            return oldItem == newItem
        }
    }
}
