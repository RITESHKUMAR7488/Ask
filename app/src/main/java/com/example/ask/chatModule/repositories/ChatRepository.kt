package com.example.ask.chatModule.repositories

import com.example.ask.chatModule.models.ChatModel
import com.example.ask.chatModule.models.ChatRoomModel
import com.example.ask.chatModule.models.ParticipantInfo
import com.example.ask.utilities.UiState

interface ChatRepository {
    fun createOrGetChatRoom(
        queryId: String,
        queryTitle: String,
        queryOwnerId: String,
        queryOwnerName: String,
        currentUserId: String,
        currentUserName: String,
        result: (UiState<ChatRoomModel>) -> Unit
    )

    fun sendMessage(
        chatRoomId: String,
        message: ChatModel,
        result: (UiState<String>) -> Unit
    )

    fun getMessages(
        chatRoomId: String,
        result: (UiState<List<ChatModel>>) -> Unit
    )

    fun addMessageListener(
        chatRoomId: String,
        callback: (List<ChatModel>) -> Unit
    )

    fun removeMessageListener()

    fun markMessagesAsRead(
        chatRoomId: String,
        userId: String,
        result: (UiState<String>) -> Unit
    )
}