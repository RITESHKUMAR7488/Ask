package com.example.ask.chatModule.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatRoomModel(
    var chatRoomId: String? = null,
    var queryId: String? = null,
    var queryTitle: String? = null,
    var queryOwnerId: String? = null,
    var queryOwnerName: String? = null,
    var participants: List<String>? = emptyList(),
    var participantDetails: Map<String, ParticipantInfo>? = emptyMap(),
    var lastMessage: String? = null,
    var lastMessageTime: Long = System.currentTimeMillis(),
    var lastMessageSenderId: String? = null,
    var createdAt: Long = System.currentTimeMillis(),
    var isActive: Boolean = true
) : Parcelable

@Parcelize
data class ParticipantInfo(
    var userId: String? = null,
    var userName: String? = null,
    var userImageUrl: String? = null,
    var joinedAt: Long = System.currentTimeMillis()
) : Parcelable