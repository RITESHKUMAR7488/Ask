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
import com.example.ask.databinding.ItemNotificationBinding
import com.example.ask.notificationModule.models.NotificationModel
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val context: Context,
    private val onNotificationClick: (NotificationModel) -> Unit
) : ListAdapter<NotificationModel, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
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

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationModel) {
            with(binding) {
                tvTitle.text = notification.title
                tvMessage.text = notification.message
                tvTimestamp.text = formatTimestamp(notification.timestamp ?: 0L)

                // Setup notification type UI
                setupNotificationType(notification.type)

                // Show/hide unread indicator
                setupReadStatus(notification.isRead)

                // Setup sender info if available
                setupSenderInfo(notification)

                // Click listener
                root.setOnClickListener {
                    onNotificationClick(notification)
                }
            }
        }

        private fun setupNotificationType(type: String?) {
            with(binding) {
                when (type) {
                    "HELP_REQUEST" -> {
                        ivNotificationIcon.setImageResource(R.drawable.ic_help)
                        ivNotificationIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary_color))
                        chipType.text = "Help Request"
                        chipType.setChipBackgroundColorResource(R.color.help_chip_color)
                    }
                    "QUERY_UPDATE" -> {
                        ivNotificationIcon.setImageResource(R.drawable.ic_update)
                        ivNotificationIcon.setColorFilter(ContextCompat.getColor(context, R.color.success_color))
                        chipType.text = "Query Update"
                        chipType.setChipBackgroundColorResource(R.color.update_chip_color)
                    }
                    "COMMUNITY_INVITE" -> {
                        ivNotificationIcon.setImageResource(R.drawable.ic_group)
                        ivNotificationIcon.setColorFilter(ContextCompat.getColor(context, R.color.secondary_color))
                        chipType.text = "Community"
                        chipType.setChipBackgroundColorResource(R.color.community_chip_color)
                    }
                    "RESPONSE" -> {
                        ivNotificationIcon.setImageResource(R.drawable.ic_comment)
                        ivNotificationIcon.setColorFilter(ContextCompat.getColor(context, R.color.info_color))
                        chipType.text = "Response"
                        chipType.setChipBackgroundColorResource(R.color.response_chip_color)
                    }
                    else -> {
                        ivNotificationIcon.setImageResource(R.drawable.ic_notification)
                        ivNotificationIcon.setColorFilter(ContextCompat.getColor(context, R.color.gray_600))
                        chipType.text = "Notification"
                        chipType.setChipBackgroundColorResource(R.color.chip_background)
                    }
                }
            }
        }

        private fun setupReadStatus(isRead: Boolean) {
            with(binding) {
                if (isRead) {
                    // Read notification styling
                    root.alpha = 0.75f
                    viewUnreadIndicator.visibility = View.GONE
                    tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    tvMessage.setTextColor(ContextCompat.getColor(context, R.color.gray_500))
                } else {
                    // Unread notification styling
                    root.alpha = 1.0f
                    viewUnreadIndicator.visibility = View.VISIBLE
                    tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    tvMessage.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
            }
        }

        private fun setupSenderInfo(notification: NotificationModel) {
            with(binding) {
                if (!notification.senderUserName.isNullOrEmpty()) {
                    layoutSenderInfo.visibility = View.VISIBLE
                    tvSenderName.text = notification.senderUserName

                    // Load sender profile image if available
                    if (!notification.senderProfileImage.isNullOrEmpty()) {
                        Glide.with(context)
                            .load(notification.senderProfileImage)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(ivSenderProfile)
                    } else {
                        ivSenderProfile.setImageResource(R.drawable.ic_person)
                    }
                } else {
                    layoutSenderInfo.visibility = View.GONE
                }
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            if (timestamp <= 0) return "Unknown"

            val now = System.currentTimeMillis()
            val difference = now - timestamp

            return when {
                difference < 60_000 -> "Just now"
                difference < 3600_000 -> "${difference / 60_000}m ago"
                difference < 86400_000 -> "${difference / 3600_000}h ago"
                difference < 604800_000 -> "${difference / 86400_000}d ago"
                difference < 2_592_000_000 -> "${difference / 604800_000}w ago" // 30 days
                else -> {
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationModel>() {
        override fun areItemsTheSame(oldItem: NotificationModel, newItem: NotificationModel): Boolean {
            return oldItem.notificationId == newItem.notificationId
        }

        override fun areContentsTheSame(oldItem: NotificationModel, newItem: NotificationModel): Boolean {
            return oldItem == newItem
        }
    }

    // Helper function to get unread count
    fun getUnreadCount(): Int {
        return currentList.count { !it.isRead }
    }

    // Helper function to mark all as read (UI update)
    fun markAllAsRead() {
        val updatedList = currentList.map { it.copy(isRead = true) }
        submitList(updatedList)
    }
}