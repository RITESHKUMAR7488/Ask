package com.example.ask.chatModule.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.firebase.Timestamp

@Parcelize
data class ChatModel(
    var messageId: String? = null,
    var senderId: String? = null,
    var senderName: String? = null,
    var senderImageUrl: String? = null,
    var message: String? = null,
    var timestamp: Long = System.currentTimeMillis(),
    var messageType: String = "TEXT", // TEXT, IMAGE, etc.
    var imageUrl: String? = null,
    var isRead: Boolean = false
) : Parcelable