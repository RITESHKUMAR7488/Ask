package com.example.ask.notificationModule.utils

import android.util.Patterns
import com.example.ask.notificationModule.models.NotificationModel

object NotificationUtils {

    /**
     * Creates an enhanced help request notification with contact details
     */
    fun createHelpRequestNotification(
        targetUserId: String,
        queryTitle: String,
        queryId: String,
        senderUserId: String,
        senderUserName: String,
        senderPhoneNumber: String? = null,
        senderEmail: String? = null,
        senderProfileImage: String? = null
    ): NotificationModel {

        // Validate and clean contact info
        val validPhone = senderPhoneNumber?.trim()?.takeIf {
            it.isNotEmpty() && it.replace(Regex("[^\\d+]"), "").length >= 7
        }

        val validEmail = senderEmail?.trim()?.takeIf {
            it.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(it).matches()
        }

        // Create enhanced contact message
        val contactInfo = buildString {
            if (!validPhone.isNullOrEmpty()) {
                append("📞 Phone: $validPhone")
            }
            if (!validEmail.isNullOrEmpty()) {
                if (isNotEmpty()) append("\n")
                append("📧 Email: $validEmail")
            }
        }

        val message = when {
            contactInfo.isNotEmpty() -> {
                "🤝 Hi! I can help you with '$queryTitle'.\n\nContact me:\n$contactInfo"
            }
            else -> {
                "🤝 Hi! I can help you with '$queryTitle'. Let me know if you need assistance!"
            }
        }

        return NotificationModel(
            userId = targetUserId,
            title = "🆘 Someone wants to help!",
            message = message,
            type = "HELP_REQUEST",
            queryId = queryId,
            senderUserId = senderUserId,
            senderUserName = senderUserName,
            senderProfileImage = senderProfileImage,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
    }

    /**
     * Creates a query update notification
     */
    fun createQueryUpdateNotification(
        targetUserId: String,
        queryTitle: String,
        queryId: String,
        updateMessage: String,
        senderUserId: String? = null,
        senderUserName: String? = null
    ): NotificationModel {
        return NotificationModel(
            userId = targetUserId,
            title = "🔄 Query Update",
            message = "Update on '$queryTitle': $updateMessage",
            type = "QUERY_UPDATE",
            queryId = queryId,
            senderUserId = senderUserId,
            senderUserName = senderUserName,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
    }

    /**
     * Creates a response notification
     */
    fun createResponseNotification(
        targetUserId: String,
        queryTitle: String,
        queryId: String,
        senderUserId: String,
        senderUserName: String,
        senderProfileImage: String? = null
    ): NotificationModel {
        return NotificationModel(
            userId = targetUserId,
            title = "💬 New Response",
            message = "$senderUserName responded to your query: '$queryTitle'",
            type = "RESPONSE",
            queryId = queryId,
            senderUserId = senderUserId,
            senderUserName = senderUserName,
            senderProfileImage = senderProfileImage,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
    }

    /**
     * Creates a community invitation notification
     */
    fun createCommunityInviteNotification(
        targetUserId: String,
        communityName: String,
        communityId: String,
        senderUserId: String,
        senderUserName: String,
        senderProfileImage: String? = null
    ): NotificationModel {
        return NotificationModel(
            userId = targetUserId,
            title = "👥 Community Invitation",
            message = "$senderUserName invited you to join '$communityName' community",
            type = "COMMUNITY_INVITE",
            communityId = communityId,
            senderUserId = senderUserId,
            senderUserName = senderUserName,
            senderProfileImage = senderProfileImage,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
    }

    /**
     * Creates a generic notification
     */
    fun createGenericNotification(
        targetUserId: String,
        title: String,
        message: String,
        type: String = "GENERAL",
        actionData: String? = null
    ): NotificationModel {
        return NotificationModel(
            userId = targetUserId,
            title = "📢 $title",
            message = message,
            type = type,
            actionData = actionData,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
    }

    /**
     * Enhanced time formatting
     */
    fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val difference = now - timestamp

        return when {
            difference < 60_000 -> "Just now"
            difference < 3600_000 -> "${difference / 60_000}m ago"
            difference < 86400_000 -> "${difference / 3600_000}h ago"
            difference < 604800_000 -> "${difference / 86400_000}d ago"
            difference < 2_592_000_000 -> "${difference / 604800_000}w ago" // 30 days
            else -> {
                val days = difference / 86400_000
                when {
                    days < 365 -> "${days}d ago"
                    else -> "${days / 365}y ago"
                }
            }
        }
    }

    /**
     * Enhanced validation
     */
    fun isValidNotification(notification: NotificationModel): Boolean {
        return !notification.userId.isNullOrEmpty() &&
                !notification.title.isNullOrEmpty() &&
                !notification.message.isNullOrEmpty() &&
                !notification.type.isNullOrEmpty()
    }

    /**
     * Gets notification priority based on type
     */
    fun getNotificationPriority(type: String): Int {
        return when (type) {
            "HELP_REQUEST" -> 3 // High priority
            "RESPONSE" -> 2 // Medium priority
            "QUERY_UPDATE" -> 2 // Medium priority
            "COMMUNITY_INVITE" -> 1 // Low priority
            else -> 0 // Lowest priority
        }
    }

    /**
     * Extract contact info from notification message
     */
    fun extractContactInfo(message: String): Pair<String?, String?> {
        val phoneRegex = Regex("📞\\s*(?:Phone:|Call:)?\\s*([+]?[0-9\\s()-]{7,15})")
        val emailRegex = Regex("📧\\s*(?:Email:|Mail:)?\\s*([\\w._%+-]+@[\\w.-]+\\.[A-Z|a-z]{2,})")

        val phoneNumber = phoneRegex.find(message)?.groupValues?.get(1)?.trim()?.replace(Regex("[\\s()-]"), "")
        val email = emailRegex.find(message)?.groupValues?.get(1)?.trim()

        return Pair(phoneNumber, email)
    }

    /**
     * Check if notification contains contact info
     */
    fun hasContactInfo(notification: NotificationModel): Boolean {
        val message = notification.message ?: ""
        return message.contains("📞") || message.contains("📧") ||
                message.contains("Phone:") || message.contains("Email:")
    }

    /**
     * Get notification summary for display
     */
    fun getNotificationSummary(notification: NotificationModel): String {
        return when (notification.type) {
            "HELP_REQUEST" -> "Someone wants to help with your query"
            "RESPONSE" -> "New response to your query"
            "QUERY_UPDATE" -> "Your query has been updated"
            "COMMUNITY_INVITE" -> "New community invitation"
            else -> notification.message ?: "New notification"
        }
    }
}