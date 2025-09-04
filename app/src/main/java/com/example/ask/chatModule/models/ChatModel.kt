// ChatModel.kt
package com.example.ask.chatModule.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class ChatRoomModel(
    var chatRoomId: String? = null,
    var participants: List<String>? = null, // List of user IDs
    var participantNames: Map<String, String>? = null, // Map of userId to userName
    var participantImages: Map<String, String>? = null, // Map of userId to userImageUrl
    var queryId: String? = null, // The original query that started this chat
    var queryTitle: String? = null,
    var lastMessage: String? = null,
    var lastMessageTime: Long? = null,
    var lastMessageSenderId: String? = null,
    var createdAt: Long? = null,
    var isActive: Boolean = true
) : Parcelable

@Parcelize
data class MessageModel(
    var messageId: String? = null,
    var chatRoomId: String? = null,
    var senderId: String? = null,
    var senderName: String? = null,
    var senderImage: String? = null,
    var message: String? = null,
    var timestamp: Long? = null,
    var messageType: String = "TEXT", // TEXT, IMAGE, FILE
    var imageUrl: String? = null,
    var isRead: Boolean = false,
    var readBy: List<String>? = null // List of user IDs who read this message
) : Parcelable

@Parcelize
data class ChatParticipant(
    var userId: String? = null,
    var userName: String? = null,
    var userImage: String? = null,
    var isOnline: Boolean = false,
    var lastSeen: Long? = null
) : Parcelable