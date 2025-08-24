package com.example.ask.notificationModule.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NotificationModel(
    var notificationId: String? = null,
    var userId: String? = null,
    var title: String? = null,
    var message: String? = null,
    var type: String? = null, // HELP_REQUEST, QUERY_UPDATE, COMMUNITY_INVITE, etc.
    var timestamp: Long? = null,
    var isRead: Boolean = false,
    var queryId: String? = null,
    var communityId: String? = null,
    var senderUserId: String? = null,
    var senderUserName: String? = null,
    var senderProfileImage: String? = null,
    var actionData: String? = null // JSON string for additional data
) : Parcelable