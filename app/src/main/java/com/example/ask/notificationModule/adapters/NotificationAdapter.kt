package com.example.ask.notificationModule.adapters

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
// Use the correct binding class generated from item_notification.xml
import com.example.ask.databinding.ItemNotificationBinding
import com.example.ask.notificationModule.models.NotificationModel
// Import the utils to format time
import com.example.ask.notificationModule.utils.NotificationUtils
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val context: Context,
    private val onNotificationClick: (NotificationModel) -> Unit
) : ListAdapter<NotificationModel, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        // Inflate using the correct binding class
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(
        // Use the correct binding class
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationModel) {
            with(binding) {
                // --- BIND DATA TO CORRECT VIEW IDs ---
                notificationTitle.text = notification.title // Use notificationTitle
                notificationMessage.text = notification.message // Use notificationMessage
                notificationTime.text = NotificationUtils.getTimeAgo(notification.timestamp ?: 0L) // Use notificationTime and util

                // Setup notification type UI (Icon and Chip)
                setupNotificationType(notification.type)

                // Show/hide unread indicator (Use unreadIndicator)
                unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE

                // Adjust alpha based on read status
                notificationCard.alpha = if (notification.isRead) 0.7f else 1.0f

                // Click listener
                notificationCard.setOnClickListener { // Set listener on the card
                    onNotificationClick(notification)
                }
                // --- END BINDING ---
            }
        }

        private fun setupNotificationType(type: String?) {
            with(binding) {
                // Use notificationIcon and notificationTypeChip
                when (type) {
                    "HELP_REQUEST" -> {
                        notificationIcon.setImageResource(R.drawable.ic_help)
                        iconContainer.backgroundTintList = ContextCompat.getColorStateList(context, R.color.help_chip_color) // Example color
                        notificationTypeChip.text = "Help"
                        notificationTypeChip.visibility = View.VISIBLE
                    }
                    "QUERY_UPDATE" -> {
                        notificationIcon.setImageResource(R.drawable.ic_update)
                        iconContainer.backgroundTintList = ContextCompat.getColorStateList(context, R.color.update_chip_color) // Example color
                        notificationTypeChip.text = "Update"
                        notificationTypeChip.visibility = View.VISIBLE
                    }
                    "COMMUNITY_INVITE" -> {
                        notificationIcon.setImageResource(R.drawable.ic_group)
                        iconContainer.backgroundTintList = ContextCompat.getColorStateList(context, R.color.community_chip_color) // Example color
                        notificationTypeChip.text = "Invite"
                        notificationTypeChip.visibility = View.VISIBLE
                    }
                    "RESPONSE" -> {
                        notificationIcon.setImageResource(R.drawable.ic_comment)
                        iconContainer.backgroundTintList = ContextCompat.getColorStateList(context, R.color.response_chip_color) // Example color
                        notificationTypeChip.text = "Response"
                        notificationTypeChip.visibility = View.VISIBLE
                    }
                    "CHAT" -> {
                        notificationIcon.setImageResource(R.drawable.ic_chat) // Assuming you have an ic_chat icon
                        iconContainer.backgroundTintList = ContextCompat.getColorStateList(context, R.color.info_color) // Example color
                        notificationTypeChip.text = "Chat"
                        notificationTypeChip.visibility = View.VISIBLE
                    }
                    // Add cases for other types like COMMUNITY_JOIN if needed
                    else -> {
                        notificationIcon.setImageResource(R.drawable.ic_notification)
                        iconContainer.backgroundTintList = ContextCompat.getColorStateList(context, R.color.gray_300) // Default color
                        notificationTypeChip.visibility = View.GONE // Hide chip for generic/unknown
                    }
                }
                // Ensure icon tint matches container for better visuals if needed
                notificationIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.white) // Assuming white icon on colored background
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationModel>() {
        override fun areItemsTheSame(oldItem: NotificationModel, newItem: NotificationModel): Boolean {
            return oldItem.notificationId == newItem.notificationId
        }

        override fun areContentsTheSame(oldItem: NotificationModel, newItem: NotificationModel): Boolean {
            return oldItem == newItem // Relies on NotificationModel being a data class
        }
    }
}