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
import com.example.ask.chatModule.models.MessageModel
import com.example.ask.databinding.ItemMessageReceivedBinding
import com.example.ask.databinding.ItemMessageSentBinding
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val context: Context,
    private val currentUserId: String,
    private val onMessageLongClick: (MessageModel) -> Unit
) : ListAdapter<MessageModel, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReceivedMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    inner class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageModel) {
            with(binding) {
                // Message content
                tvMessage.text = message.message

                // Timestamp
                tvTimestamp.text = formatTimestamp(message.timestamp ?: 0)

                // Read status
                if (message.isRead) {
                    ivReadStatus.setImageResource(R.drawable.ic_double_check)
                } else {
                    ivReadStatus.setImageResource(R.drawable.ic_single_check)
                }

                // Edit indicator
                if (message.isEdited) {
                    tvEdited.visibility = View.VISIBLE
                } else {
                    tvEdited.visibility = View.GONE
                }

                // Handle media messages
                if (message.messageType == "image" && !message.mediaUrl.isNullOrEmpty()) {
                    ivMedia.visibility = View.VISIBLE
                    tvMessage.visibility = View.GONE

                    Glide.with(context)
                        .load(message.mediaUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(ivMedia)
                } else {
                    ivMedia.visibility = View.GONE
                    tvMessage.visibility = View.VISIBLE
                }

                // Long click for options
                root.setOnLongClickListener {
                    onMessageLongClick(message)
                    true
                }
            }
        }
    }

    inner class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageModel) {
            with(binding) {
                // Sender info
                tvSenderName.text = message.senderName

                // Message content
                tvMessage.text = message.message

                // Timestamp
                tvTimestamp.text = formatTimestamp(message.timestamp ?: 0)

                // Sender avatar
                if (!message.senderImage.isNullOrEmpty()) {
                    Glide.with(context)
                        .load(message.senderImage)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivSenderAvatar)
                } else {
                    ivSenderAvatar.setImageResource(R.drawable.ic_person)
                }

                // Edit indicator
                if (message.isEdited) {
                    tvEdited.visibility = View.VISIBLE
                } else {
                    tvEdited.visibility = View.GONE
                }

                // Handle media messages
                if (message.messageType == "image" && !message.mediaUrl.isNullOrEmpty()) {
                    ivMedia.visibility = View.VISIBLE
                    tvMessage.visibility = View.GONE

                    Glide.with(context)
                        .load(message.mediaUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(ivMedia)
                } else {
                    ivMedia.visibility = View.GONE
                    tvMessage.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val difference = now - timestamp

        return when {
            difference < 60_000 -> "Just now"
            difference < 3600_000 -> "${difference / 60_000}m ago"
            difference < 86400_000 -> {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
            difference < 604800_000 -> {
                val sdf = SimpleDateFormat("EEE HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
            else -> {
                val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<MessageModel>() {
        override fun areItemsTheSame(oldItem: MessageModel, newItem: MessageModel): Boolean {
            return oldItem.messageId == newItem.messageId
        }

        override fun areContentsTheSame(oldItem: MessageModel, newItem: MessageModel): Boolean {
            return oldItem == newItem
        }
    }
}