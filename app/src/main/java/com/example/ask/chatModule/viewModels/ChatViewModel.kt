package com.example.ask.chatModule.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ask.chatModule.models.ChatModel
import com.example.ask.chatModule.models.MessageModel
import com.example.ask.chatModule.repositories.ChatRepository
import com.example.ask.utilities.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject // IMPORTANT: Using javax.inject, not jakarta.inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _createChatState = MutableLiveData<UiState<ChatModel>>()
    val createChatState: LiveData<UiState<ChatModel>> = _createChatState

    private val _userChatsState = MutableLiveData<UiState<List<ChatModel>>>()
    val userChatsState: LiveData<UiState<List<ChatModel>>> = _userChatsState

    private val _sendMessageState = MutableLiveData<UiState<String>>()
    val sendMessageState: LiveData<UiState<String>> = _sendMessageState

    private val _messagesFlow = MutableStateFlow<List<MessageModel>>(emptyList())
    val messagesFlow: StateFlow<List<MessageModel>> = _messagesFlow.asStateFlow()

    private val _deleteMessageState = MutableLiveData<UiState<String>>()
    val deleteMessageState: LiveData<UiState<String>> = _deleteMessageState

    private val _editMessageState = MutableLiveData<UiState<String>>()
    val editMessageState: LiveData<UiState<String>> = _editMessageState

    fun createOrGetChat(
        queryId: String,
        queryTitle: String,
        participantIds: List<String>,
        participantNames: Map<String, String>,
        participantImages: Map<String, String>
    ) {
        _createChatState.value = UiState.Loading
        chatRepository.createOrGetChat(
            queryId = queryId,
            queryTitle = queryTitle,
            participantIds = participantIds,
            participantNames = participantNames,
            participantImages = participantImages
        ) { state ->
            _createChatState.value = state
        }
    }

    fun getUserChats(userId: String) {
        _userChatsState.value = UiState.Loading
        chatRepository.getUserChats(userId) { state ->
            _userChatsState.value = state
        }
    }

    fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        senderImage: String?,
        message: String,
        messageType: String = "text",
        mediaUrl: String? = null
    ) {
        val messageModel = MessageModel(
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            senderImage = senderImage,
            message = message,
            messageType = messageType,
            mediaUrl = mediaUrl,
            isRead = false,
            isEdited = false
        )

        _sendMessageState.value = UiState.Loading
        chatRepository.sendMessage(chatId, messageModel) { state ->
            _sendMessageState.value = state
        }
    }

    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { messages ->
                _messagesFlow.value = messages
            }
        }
    }

    fun markMessagesAsRead(chatId: String, userId: String) {
        chatRepository.markMessagesAsRead(chatId, userId)
    }

    fun deleteMessage(chatId: String, messageId: String) {
        _deleteMessageState.value = UiState.Loading
        chatRepository.deleteMessage(chatId, messageId) { state ->
            _deleteMessageState.value = state
        }
    }

    fun editMessage(chatId: String, messageId: String, newMessage: String) {
        _editMessageState.value = UiState.Loading
        chatRepository.editMessage(chatId, messageId, newMessage) { state ->
            _editMessageState.value = state
        }
    }
}