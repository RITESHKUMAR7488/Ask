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

    inner class ChatRoomViewHolder(
        private val binding: ItemChatRoomBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chatRoom: ChatRoomModel) {
            with(binding) {
                // Determine the other participant (not current user)
                val otherParticipant = getOtherParticipant(chatRoom)

                // Set chat room info
                tvChatTitle.text = chatRoom.queryTitle ?: "Unknown Query"
                tvLastMessage.text = chatRoom.lastMessage ?: "No messages yet"
                tvTimestamp.text = formatTimestamp(chatRoom.lastMessageTime)

                // Set participant info
                tvParticipantName.text = otherParticipant?.userName ?: "Unknown User"

                // Load participant profile image
                if (!otherParticipant?.userImageUrl.isNullOrEmpty()) {
                    Glide.with(context)
                        .load(otherParticipant?.userImageUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivParticipantImage)
                } else {
                    ivParticipantImage.setImageResource(R.drawable.ic_person)
                }

                // Show unread indicator if last message sender is not current user
                if (chatRoom.lastMessageSenderId != currentUserId &&
                    chatRoom.lastMessageSenderId != null) {
                    ivUnreadIndicator.visibility = View.VISIBLE
                } else {
                    ivUnreadIndicator.visibility = View.GONE
                }

                // Set click listener
                root.setOnClickListener {
                    onChatRoomClick(chatRoom)
                }
            }
        }

        private fun getOtherParticipant(chatRoom: ChatRoomModel): com.example.ask.chatModule.models.ParticipantInfo? {
            return chatRoom.participantDetails?.values?.firstOrNull { participant ->
                participant.userId != currentUserId
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