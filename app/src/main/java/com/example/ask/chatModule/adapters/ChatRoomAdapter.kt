package com.example.ask.chatModule.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ask.R
import com.example.ask.chatModule.models.ChatRoomModel
import com.example.ask.databinding.ItemChatRoomBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatRoomAdapter(
    private val context: Context,
    private val currentUserId: String,
    private val onChatRoomClick: (ChatRoomModel) -> Unit
) : ListAdapter<ChatRoomModel, ChatRoomAdapter.ChatRoomViewHolder>(ChatRoomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val binding = ItemChatRoomBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatRoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatRoomViewHolder(private val binding: ItemChatRoomBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chatRoom: ChatRoomModel) {
            with(binding) {
                // Get other participant's info
                val otherParticipantId = chatRoom.participants?.firstOrNull { it != currentUserId }
                val otherParticipantName = otherParticipantId?.let { chatRoom.participantNames?.get(it) }
                val otherParticipantImage = otherParticipantId?.let { chatRoom.participantImages?.get(it) }

                // Set participant name
                tvParticipantName.text = otherParticipantName ?: "Unknown User"

                // Set last message
                tvLastMessage.text = chatRoom.lastMessage ?: "No messages yet"

                // Set timestamp
                chatRoom.lastMessageTime?.let { timestamp ->
                    tvTimestamp.text = formatTimestamp(timestamp)
                }

                // Load participant image
                if (!otherParticipantImage.isNullOrEmpty()) {
                    Glide.with(context)
                        .load(otherParticipantImage)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivParticipantImage)
                } else {
                    ivParticipantImage.setImageResource(R.drawable.ic_person)
                }

                // Show query info if available
                if (!chatRoom.queryTitle.isNullOrEmpty()) {
                    tvQueryTitle.text = "About: ${chatRoom.queryTitle}"
                    tvQueryTitle.visibility = android.view.View.VISIBLE
                } else {
                    tvQueryTitle.visibility = android.view.View.GONE
                }

                // Set click listener
                root.setOnClickListener {
                    onChatRoomClick(chatRoom)
                }
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
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    class ChatRoomDiffCallback : DiffUtil.ItemCallback<ChatRoomModel>() {
        override fun areItemsTheSame(oldItem: ChatRoomModel, newItem: ChatRoomModel): Boolean {
            return oldItem.chatRoomId == newItem.chatRoomId
        }

        override fun areContentsTheSame(oldItem: ChatRoomModel, newItem: ChatRoomModel): Boolean {
            return oldItem == newItem
        }
    }
}