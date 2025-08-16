package com.example.ask.communityModule.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.example.ask.communityModule.models.CommunityModels
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ask.databinding.CommunityItemBinding



class MyCommunityAdapter(
    private val onCommunityClick: (CommunityModels) -> Unit
) : ListAdapter<CommunityModels, MyCommunityAdapter.CommunityViewHolder>(CmmunityDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyCommunityAdapter.CommunityViewHolder {
        val binding = CommunityItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommunityViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MyCommunityAdapter.CommunityViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    inner class CommunityViewHolder(private val binding: CommunityItemBinding) :
        RecyclerView.ViewHolder(binding.root){
        fun bind(community: CommunityModels){
            with(binding){
                tvCommunityName.text=community.communityName
                tvRole.text=community.role?.uppercase()
                tvJoinedDate.text= formatJoinedDate(community.joinedAt)
                when (community.role) {
                    "admin" -> {
                        tvRole.setBackgroundResource(android.R.color.holo_red_light)
                    }
                    "member" -> {
                        tvRole.setBackgroundResource(android.R.color.holo_blue_light)
                    }
                }
                root.setOnClickListener {
                    onCommunityClick(community)
                }

            }
        }
        private fun formatJoinedDate(timestamp: Long?): String {
            if (timestamp == null) return "Unknown"

            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000} minutes ago"
                diff < 86400000 -> "${diff / 3600000} hours ago"
                diff < 604800000 -> "${diff / 86400000} days ago"
                else -> "${diff / 604800000} weeks ago"
            }
        }

        }
    private class CmmunityDiffCallback : DiffUtil.ItemCallback<CommunityModels>() {
        override fun areItemsTheSame(oldItem: CommunityModels, newItem: CommunityModels): Boolean {
            return oldItem.communityId == newItem.communityId
        }

        override fun areContentsTheSame(oldItem: CommunityModels, newItem: CommunityModels): Boolean {
            return oldItem == newItem
        }
    }
}