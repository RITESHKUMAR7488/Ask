package com.example.ask.chatModule.repositories

import com.example.ask.chatModule.models.ChatRoomModel
import com.example.ask.chatModule.models.MessageModel
import com.example.ask.utilities.UiState

interface ChatRepository {
    fun createChatRoom(
        chatRoom: ChatRoomModel,
        result: (UiState<ChatRoomModel>) -> Unit
    )

    fun getChatRooms(
        userId: String,
        result: (UiState<List<ChatRoomModel>>) -> Unit
    )

    fun sendMessage(
        message: MessageModel,
        result: (UiState<String>) -> Unit
    )

    fun getMessages(
        chatRoomId: String,
        result: (UiState<List<MessageModel>>) -> Unit
    )

    fun listenToMessages(
        chatRoomId: String,
        onMessageReceived: (List<MessageModel>) -> Unit
    )

    fun markMessageAsRead(
        chatRoomId: String,
        messageId: String,
        userId: String
    )

    fun checkExistingChatRoom(
        currentUserId: String,
        targetUserId: String,
        result: (UiState<ChatRoomModel?>) -> Unit
    )

    fun removeMessageListener()
}