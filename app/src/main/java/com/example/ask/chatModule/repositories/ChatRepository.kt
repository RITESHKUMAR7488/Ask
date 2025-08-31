package com.example.ask.chatModule.repositories

import com.example.ask.chatModule.models.ChatModel
import com.example.ask.chatModule.models.MessageModel
import com.example.ask.utilities.UiState
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun createOrGetChat(
        queryId: String,
        queryTitle: String,
        participantIds: List<String>,
        participantNames: Map<String, String>,
        participantImages: Map<String, String>,
        result: (UiState<ChatModel>) -> Unit
    )

    fun getUserChats(
        userId: String,
        result: (UiState<List<ChatModel>>) -> Unit
    )

    fun sendMessage(
        chatId: String,
        message: MessageModel,
        result: (UiState<String>) -> Unit
    )

    fun getMessages(
        chatId: String
    ): Flow<List<MessageModel>>

    fun markMessagesAsRead(
        chatId: String,
        userId: String
    )

    fun deleteMessage(
        chatId: String,
        messageId: String,
        result: (UiState<String>) -> Unit
    )

    fun editMessage(
        chatId: String,
        messageId: String,
        newMessage: String,
        result: (UiState<String>) -> Unit
    )
}