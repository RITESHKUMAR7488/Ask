package com.example.ask.chatModule.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatModel(
    var chatId: String? = null,
    var queryId: String? = null,
    var queryTitle: String? = null,
    var participants: List<String>? = null, // List of user IDs
    var participantNames: Map<String, String>? = null, // userId to userName mapping
    var participantImages: Map<String, String>? = null, // userId to imageUrl mapping
    var lastMessage: String? = null,
    var lastMessageTime: Long? = null,
    var lastMessageSenderId: String? = null,
    var unreadCount: Map<String, Int>? = null, // userId to unread count mapping
    var createdAt: Long? = null,
    var isActive: Boolean = true
) : Parcelable

@Parcelize
data class MessageModel(
    var messageId: String? = null,
    var chatId: String? = null,
    var senderId: String? = null,
    var senderName: String? = null,
    var senderImage: String? = null,
    var message: String? = null,
    var timestamp: Long? = null,
    var isRead: Boolean = false,
    var messageType: String = "text", // text, image, file
    var mediaUrl: String? = null,
    var isEdited: Boolean = false,
    var editedAt: Long? = null
) : Parcelable