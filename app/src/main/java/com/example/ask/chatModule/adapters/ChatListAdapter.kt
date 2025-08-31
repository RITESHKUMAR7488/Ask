package com.example.ask.chatModule.adapters

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ask.R
import com.example.ask.chatModule.models.ChatModel
import com.example.ask.databinding.ItemChatListBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(
    private val context: Context,
    private val currentUserId: String,
    private val onChatClick: (ChatModel) -> Unit
) : ListAdapter<ChatModel, ChatListAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(
        private val binding: ItemChatListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: ChatModel) {
            with(binding) {
                // Find the other participant
                val otherUserId = chat.participants?.find { it != currentUserId }
                val otherUserName = chat.participantNames?.get(otherUserId) ?: "Unknown User"
                val otherUserImage = chat.participantImages?.get(otherUserId)

                // Set user info
                tvUserName.text = otherUserName
                tvQueryTitle.text = chat.queryTitle ?: "Query Chat"

                // Set last message
                if (!chat.lastMessage.isNullOrEmpty()) {
                    tvLastMessage.text = if (chat.lastMessageSenderId == currentUserId) {
                        "You: ${chat.lastMessage}"
                    } else {
                        chat.lastMessage
                    }
                    tvLastMessage.visibility = View.VISIBLE
                } else {
                    tvLastMessage.visibility = View.GONE
                }

                // Set timestamp
                chat.lastMessageTime?.let { timestamp ->
                    tvTimestamp.text = formatTimestamp(timestamp)
                    tvTimestamp.visibility = View.VISIBLE
                } ?: run {
                    tvTimestamp.visibility = View.GONE
                }

                // Set unread count
                val unreadCount = chat.unreadCount?.get(currentUserId) ?: 0
                if (unreadCount > 0) {
                    tvUnreadCount.visibility = View.VISIBLE
                    tvUnreadCount.text = if (unreadCount > 99) "99+" else unreadCount.toString()

                    // Make text bold for unread messages
                    tvUserName.setTypeface(null, Typeface.BOLD)
                    tvLastMessage.setTypeface(null, Typeface.BOLD)
                } else {
                    tvUnreadCount.visibility = View.GONE

                    // Normal text for read messages
                    tvUserName.setTypeface(null, Typeface.NORMAL)
                    tvLastMessage.setTypeface(null, Typeface.NORMAL)
                }

                // Load user avatar
                if (!otherUserImage.isNullOrEmpty()) {
                    Glide.with(context)
                        .load(otherUserImage)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivUserAvatar)
                } else {
                    ivUserAvatar.setImageResource(R.drawable.ic_person)
                }

                // Click listener
                root.setOnClickListener {
                    onChatClick(chat)
                }
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val difference = now - timestamp

            return when {
                difference < 60_000 -> "Now"
                difference < 3600_000 -> "${difference / 60_000}m"
                difference < 86400_000 -> {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
                difference < 172800_000 -> "Yesterday"
                difference < 604800_000 -> {
                    val sdf = SimpleDateFormat("EEE", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
                else -> {
                    val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<ChatModel>() {
        override fun areItemsTheSame(oldItem: ChatModel, newItem: ChatModel): Boolean {
            return oldItem.chatId == newItem.chatId
        }

        override fun areContentsTheSame(oldItem: ChatModel, newItem: ChatModel): Boolean {
            return oldItem == newItem
        }
    }
}