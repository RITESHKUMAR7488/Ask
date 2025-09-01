package com.example.ask.chatModule.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ask.chatModule.models.ChatModel
import com.example.ask.chatModule.models.ChatRoomModel
import com.example.ask.chatModule.repositories.ChatRepository
import com.example.ask.utilities.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _chatRoom = MutableLiveData<UiState<ChatRoomModel>>()
    val chatRoom: LiveData<UiState<ChatRoomModel>> = _chatRoom

    private val _messages = MutableLiveData<UiState<List<ChatModel>>>()
    val messages: LiveData<UiState<List<ChatModel>>> = _messages

    private val _sendMessage = MutableLiveData<UiState<String>>()
    val sendMessage: LiveData<UiState<String>> = _sendMessage

    private val _markAsRead = MutableLiveData<UiState<String>>()
    val markAsRead: LiveData<UiState<String>> = _markAsRead

    // Real-time messages (not wrapped in UiState as it's for real-time updates)
    private val _realTimeMessages = MutableLiveData<List<ChatModel>>()
    val realTimeMessages: LiveData<List<ChatModel>> = _realTimeMessages

    fun createOrGetChatRoom(
        queryId: String,
        queryTitle: String,
        queryOwnerId: String,
        queryOwnerName: String,
        currentUserId: String,
        currentUserName: String
    ) {
        Log.d(TAG, "createOrGetChatRoom called")
        _chatRoom.value = UiState.Loading

        chatRepository.createOrGetChatRoom(
            queryId = queryId,
            queryTitle = queryTitle,
            queryOwnerId = queryOwnerId,
            queryOwnerName = queryOwnerName,
            currentUserId = currentUserId,
            currentUserName = currentUserName
        ) { state ->
            _chatRoom.value = state

            // If successful, start listening for real-time messages
            if (state is UiState.Success) {
                startRealtimeMessageListener(state.data.chatRoomId!!)
            }
        }
    }

    fun sendMessage(
        chatRoomId: String,
        messageText: String,
        senderId: String,
        senderName: String,
        senderImageUrl: String? = null
    ) {
        Log.d(TAG, "sendMessage called: $messageText")
        _sendMessage.value = UiState.Loading

        val message = ChatModel(
            senderId = senderId,
            senderName = senderName,
            senderImageUrl = senderImageUrl,
            message = messageText,
            timestamp = System.currentTimeMillis(),
            messageType = "TEXT",
            isRead = false
        )

        chatRepository.sendMessage(chatRoomId, message) { state ->
            _sendMessage.value = state
        }
    }

    fun loadMessages(chatRoomId: String) {
        Log.d(TAG, "loadMessages called")
        _messages.value = UiState.Loading

        chatRepository.getMessages(chatRoomId) { state ->
            _messages.value = state
        }
    }

    private fun startRealtimeMessageListener(chatRoomId: String) {
        Log.d(TAG, "Starting real-time message listener for room: $chatRoomId")

        chatRepository.addMessageListener(chatRoomId) { messages ->
            Log.d(TAG, "Real-time update received: ${messages.size} messages")
            _realTimeMessages.value = messages
        }
    }

    fun markMessagesAsRead(chatRoomId: String, userId: String) {
        Log.d(TAG, "markMessagesAsRead called")

        chatRepository.markMessagesAsRead(chatRoomId, userId) { state ->
            _markAsRead.value = state
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, removing listeners")
        chatRepository.removeMessageListener()
    }
}