// File: app/src/main/java/com/example/ask/chatModule/adapters/ChatAdapter.kt
package com.example.ask.chatModule.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ask.R
import com.example.ask.chatModule.models.ChatModel
import com.example.ask.databinding.ItemChatMessageBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val context: Context,
    private val currentUserId: String
) : ListAdapter<ChatModel, ChatAdapter.MessageViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(
        private val binding: ItemChatMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatModel) {
            val isSentByCurrentUser = message.senderId == currentUserId

            if (isSentByCurrentUser) {
                // Show sent message layout, hide received
                binding.layoutSentMessage.visibility = View.VISIBLE
                binding.layoutReceivedMessage.visibility = View.GONE

                bindSentMessage(message)
            } else {
                // Show received message layout, hide sent
                binding.layoutReceivedMessage.visibility = View.VISIBLE
                binding.layoutSentMessage.visibility = View.GONE

                bindReceivedMessage(message)
            }
        }

        private fun bindSentMessage(message: ChatModel) {
            with(binding) {
                // Set message text
                tvSentMessage.text = message.message

                // Set timestamp
                tvSentTimestamp.text = formatTimestamp(message.timestamp)

                // Load user profile image
                if (!message.senderImageUrl.isNullOrEmpty()) {
                    Glide.with(context)
                        .load(message.senderImageUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivSentUserImage)
                } else {
                    ivSentUserImage.setImageResource(R.drawable.ic_person)
                }

                // Set message status (read/unread)
                if (message.isRead) {
                    ivMessageStatus.setImageResource(R.drawable.ic_check_double)
                    ivMessageStatus.setColorFilter(context.getColor(R.color.success_color))
                } else {
                    ivMessageStatus.setImageResource(R.drawable.ic_check)
                    ivMessageStatus.setColorFilter(context.getColor(R.color.gray_400))
                }
            }
        }

        private fun bindReceivedMessage(message: ChatModel) {
            with(binding) {
                // Set message text
                tvReceivedMessage.text = message.message

                // Set sender name
                tvReceivedSenderName.text = message.senderName ?: "Unknown User"

                // Set timestamp
                tvReceivedTimestamp.text = formatTimestamp(message.timestamp)

                // Load sender profile image
                if (!message.senderImageUrl.isNullOrEmpty()) {
                    Glide.with(context)
                        .load(message.senderImageUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivReceivedUserImage)
                } else {
                    ivReceivedUserImage.setImageResource(R.drawable.ic_person)
                }
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val difference = now - timestamp

            return when {
                difference < 60_000 -> "Just now"
                difference < 3600_000 -> {
                    val minutes = difference / 60_000
                    "${minutes}m ago"
                }
                difference < 86400_000 -> {
                    val hours = difference / 3600_000
                    "${hours}h ago"
                }
                difference < 604800_000 -> {
                    val days = difference / 86400_000
                    "${days}d ago"
                }
                else -> {
                    // Show actual date for older messages
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = timestamp

                    val today = Calendar.getInstance()

                    when {
                        calendar.get(Calendar.YEAR) != today.get(Calendar.YEAR) -> {
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
                        }
                        calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
                            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
                        }
                        calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> {
                            "Yesterday " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
                        }
                        else -> {
                            SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date(timestamp))
                        }
                    }
                }
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<ChatModel>() {
        override fun areItemsTheSame(oldItem: ChatModel, newItem: ChatModel): Boolean {
            return oldItem.messageId == newItem.messageId
        }

        override fun areContentsTheSame(oldItem: ChatModel, newItem: ChatModel): Boolean {
            return oldItem == newItem
        }
    }
}