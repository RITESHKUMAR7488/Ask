package com.example.ask.chatModule.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.example.ask.R
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val conversations: List<Conversation>,
    private val onConversationClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount(): Int = conversations.size

    inner class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userName: TextView = itemView.findViewById(R.id.tvUserName)
        private val lastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        private val timestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val unreadCount: TextView = itemView.findViewById(R.id.tvUnreadCount)

        fun bind(conversation: Conversation) {
            // Set user name
            if (conversation.conversationWith is User) {
                val user = conversation.conversationWith as User
                userName.text = user.name ?: "Unknown User"
            }

            // Set last message
            val lastMsg = conversation.lastMessage
            if (lastMsg is TextMessage) {
                lastMessage.text = lastMsg.text
            } else {
                lastMessage.text = "No messages yet"
            }

            // Set timestamp
            if (lastMsg != null) {
                timestamp.text = formatTimestamp(lastMsg.sentAt)
            } else {
                timestamp.text = ""
            }

            // Set unread count
            val unreadCountValue = conversation.unreadMessageCount
            if (unreadCountValue > 0) {
                unreadCount.visibility = View.VISIBLE
                unreadCount.text = if (unreadCountValue > 99) "99+" else unreadCountValue.toString()
            } else {
                unreadCount.visibility = View.GONE
            }

            // Set click listener
            itemView.setOnClickListener {
                onConversationClick(conversation)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis() / 1000 // Convert to seconds
            val messageTime = timestamp
            val difference = now - messageTime

            return when {
                difference < 60 -> "Just now"
                difference < 3600 -> "${difference / 60}m"
                difference < 86400 -> "${difference / 3600}h"
                difference < 604800 -> "${difference / 86400}d"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(Date(timestamp * 1000))
                }
            }
        }
    }
}