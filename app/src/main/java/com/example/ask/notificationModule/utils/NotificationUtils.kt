package com.example.ask.notificationModule.utils

import com.example.ask.notificationModule.models.NotificationModel

object NotificationUtils {

    /**
     * Creates a help request notification
     */
    fun createHelpRequestNotification(
        targetUserId: String,
        queryTitle: String,
        queryId: String,
        senderUserId: String,
        senderUserName: String,
        senderProfileImage: String? = null
    ): NotificationModel {
        return NotificationModel(
            userId = targetUserId,
            title = "Help Request",
            message = "$senderUserName is requesting help with: $queryTitle",
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
            title = "Query Update",
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
            title = "New Response",
            message = "$senderUserName responded to your query: $queryTitle",
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
            title = "Community Invitation",
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
            title = title,
            message = message,
            type = type,
            actionData = actionData,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
    }

    /**
     * Gets formatted time difference
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
     * Validates notification data
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
}