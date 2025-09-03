package com.example.ask.chatModule.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ask.chatModule.models.ChatRoomModel
import com.example.ask.chatModule.models.MessageModel
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

    // Chat Rooms
    private val _chatRooms = MutableLiveData<UiState<List<ChatRoomModel>>>()
    val chatRooms: LiveData<UiState<List<ChatRoomModel>>> = _chatRooms

    // Messages
    private val _messages = MutableLiveData<UiState<List<MessageModel>>>()
    val messages: LiveData<UiState<List<MessageModel>>> = _messages

    // Real-time messages
    private val _realTimeMessages = MutableLiveData<List<MessageModel>>()
    val realTimeMessages: LiveData<List<MessageModel>> = _realTimeMessages

    // Send message
    private val _sendMessage = MutableLiveData<UiState<String>>()
    val sendMessage: LiveData<UiState<String>> = _sendMessage

    // Create chat room
    private val _createChatRoom = MutableLiveData<UiState<ChatRoomModel>>()
    val createChatRoom: LiveData<UiState<ChatRoomModel>> = _createChatRoom

    // Check existing chat room
    private val _existingChatRoom = MutableLiveData<UiState<ChatRoomModel?>>()
    val existingChatRoom: LiveData<UiState<ChatRoomModel?>> = _existingChatRoom

    /**
     * Create or get existing chat room
     */
    fun createOrGetChatRoom(
        currentUserId: String,
        currentUserName: String,
        currentUserImage: String?,
        targetUserId: String,
        targetUserName: String,
        targetUserImage: String?,
        queryId: String?,
        queryTitle: String?
    ) {
        Log.d(TAG, "Creating or getting chat room between $currentUserId and $targetUserId")

        // First check if chat room already exists
        checkExistingChatRoom(currentUserId, targetUserId)

        // If no existing room found, create new one
        _existingChatRoom.observeForever { state ->
            when (state) {
                is UiState.Success -> {
                    if (state.data == null) {
                        // No existing chat room, create new one
                        val newChatRoom = ChatRoomModel(
                            participants = listOf(currentUserId, targetUserId),
                            participantNames = mapOf(
                                currentUserId to currentUserName,
                                targetUserId to targetUserName
                            ),
                            participantImages = mapOf(
                                currentUserId to (currentUserImage ?: ""),
                                targetUserId to (targetUserImage ?: "")
                            ),
                            queryId = queryId,
                            queryTitle = queryTitle,
                            lastMessage = "Chat started",
                            lastMessageTime = System.currentTimeMillis(),
                            lastMessageSenderId = currentUserId,
                            isActive = true
                        )

                        chatRepository.createChatRoom(newChatRoom) { createState ->
                            _createChatRoom.value = createState
                        }
                    } else {
                        // Existing chat room found
                        _createChatRoom.value = UiState.Success(state.data)
                    }
                }
                is UiState.Failure -> {
                    _createChatRoom.value = UiState.Failure(state.error)
                }
                is UiState.Loading -> {
                    _createChatRoom.value = UiState.Loading
                }
            }
        }
    }

    /**
     * Check if chat room already exists between two users
     */
    fun checkExistingChatRoom(currentUserId: String, targetUserId: String) {
        _existingChatRoom.value = UiState.Loading
        chatRepository.checkExistingChatRoom(currentUserId, targetUserId) { state ->
            _existingChatRoom.value = state
        }
    }

    /**
     * Get all chat rooms for a user
     */
    fun getChatRooms(userId: String) {
        Log.d(TAG, "Getting chat rooms for user: $userId")
        _chatRooms.value = UiState.Loading
        chatRepository.getChatRooms(userId) { state ->
            _chatRooms.value = state
        }
    }

    /**
     * Send a message
     */
    fun sendMessage(
        chatRoomId: String,
        senderId: String,
        senderName: String,
        senderImage: String?,
        messageText: String
    ) {
        Log.d(TAG, "Sending message to chat room: $chatRoomId")
        _sendMessage.value = UiState.Loading

        val message = MessageModel(
            chatRoomId = chatRoomId,
            senderId = senderId,
            senderName = senderName,
            senderImage = senderImage,
            message = messageText,
            messageType = "TEXT",
            isRead = false
        )

        chatRepository.sendMessage(message) { state ->
            _sendMessage.value = state
        }
    }

    /**
     * Get messages for a chat room (one-time fetch)
     */
    fun getMessages(chatRoomId: String) {
        Log.d(TAG, "Getting messages for chat room: $chatRoomId")
        _messages.value = UiState.Loading
        chatRepository.getMessages(chatRoomId) { state ->
            _messages.value = state
        }
    }

    /**
     * Listen to real-time messages
     */
    fun listenToMessages(chatRoomId: String) {
        Log.d(TAG, "Starting real-time message listener for chat room: $chatRoomId")
        chatRepository.listenToMessages(chatRoomId) { messages ->
            _realTimeMessages.value = messages
        }
    }

    /**
     * Mark message as read
     */
    fun markMessageAsRead(chatRoomId: String, messageId: String, userId: String) {
        chatRepository.markMessageAsRead(chatRoomId, messageId, userId)
    }

    /**
     * Remove message listener
     */
    fun removeMessageListener() {
        Log.d(TAG, "Removing message listener")
        chatRepository.removeMessageListener()
    }

    override fun onCleared() {
        super.onCleared()
        removeMessageListener()
    }
}