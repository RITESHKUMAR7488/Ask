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

    // Real-time messages
    private val _realTimeMessages = MutableLiveData<List<ChatModel>>()
    val realTimeMessages: LiveData<List<ChatModel>> = _realTimeMessages

    private var currentChatRoomId: String? = null
    private var isRealtimeListenerActive = false

    fun createOrGetChatRoom(
        queryId: String,
        queryTitle: String,
        queryOwnerId: String,
        queryOwnerName: String,
        currentUserId: String,
        currentUserName: String
    ) {
        Log.d(TAG, "createOrGetChatRoom called for queryId: $queryId")
        _chatRoom.value = UiState.Loading

        chatRepository.createOrGetChatRoom(
            queryId = queryId,
            queryTitle = queryTitle,
            queryOwnerId = queryOwnerId,
            queryOwnerName = queryOwnerName,
            currentUserId = currentUserId,
            currentUserName = currentUserName
        ) { state ->
            Log.d(TAG, "Chat room creation result: $state")
            _chatRoom.value = state

            // If successful, start listening for real-time messages
            if (state is UiState.Success) {
                currentChatRoomId = state.data.chatRoomId
                Log.d(TAG, "Starting real-time listener for room: ${state.data.chatRoomId}")
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
        Log.d(TAG, "sendMessage called: chatRoomId=$chatRoomId, text=$messageText")
        _sendMessage.value = UiState.Loading

        val message = ChatModel(
            messageId = null, // Will be generated in repository
            senderId = senderId,
            senderName = senderName,
            senderImageUrl = senderImageUrl,
            message = messageText,
            timestamp = System.currentTimeMillis(),
            messageType = "TEXT",
            isRead = false
        )

        chatRepository.sendMessage(chatRoomId, message) { state ->
            Log.d(TAG, "Send message result: $state")
            _sendMessage.value = state
        }
    }

    fun loadMessages(chatRoomId: String) {
        Log.d(TAG, "loadMessages called for room: $chatRoomId")
        _messages.value = UiState.Loading

        chatRepository.getMessages(chatRoomId) { state ->
            Log.d(TAG, "Load messages result: $state")
            _messages.value = state
        }
    }

    private fun startRealtimeMessageListener(chatRoomId: String) {
        Log.d(TAG, "Starting real-time message listener for room: $chatRoomId")

        // Prevent multiple listeners
        if (isRealtimeListenerActive && currentChatRoomId == chatRoomId) {
            Log.d(TAG, "Real-time listener already active for this room")
            return
        }

        // Remove existing listener if different room
        if (currentChatRoomId != chatRoomId) {
            stopRealtimeMessageListener()
        }

        currentChatRoomId = chatRoomId
        isRealtimeListenerActive = true

        chatRepository.addMessageListener(chatRoomId) { messages ->
            Log.d(TAG, "Real-time update received: ${messages.size} messages")
            _realTimeMessages.value = messages
        }
    }

    fun stopRealtimeMessageListener() {
        Log.d(TAG, "Stopping real-time message listener")
        chatRepository.removeMessageListener()
        isRealtimeListenerActive = false
        currentChatRoomId = null
    }

    fun markMessagesAsRead(chatRoomId: String, userId: String) {
        Log.d(TAG, "markMessagesAsRead called for room: $chatRoomId, user: $userId")

        chatRepository.markMessagesAsRead(chatRoomId, userId) { state ->
            Log.d(TAG, "Mark as read result: $state")
            _markAsRead.value = state
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, removing listeners")
        stopRealtimeMessageListener()
    }
}