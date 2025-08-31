package com.example.ask.chatModule.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ask.chatModule.repositories.ChatRepository
import com.example.ask.utilities.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _userConnection = MutableLiveData<UiState<String>>()
    val userConnection: LiveData<UiState<String>> = _userConnection

    private val _channelCreation = MutableLiveData<UiState<String>>()
    val channelCreation: LiveData<UiState<String>> = _channelCreation

    fun connectUserToChat(
        userId: String,
        userName: String,
        userEmail: String?,
        userImageUrl: String?,
        token: String
    ) {
        _userConnection.value = UiState.Loading
        chatRepository.connectUser(userId, userName, userEmail, userImageUrl, token) { state ->
            _userConnection.value = state
        }
    }

    fun createQueryChannel(
        queryId: String,
        queryTitle: String,
        queryOwnerIds: List<String>,
        helperIds: List<String>
    ) {
        _channelCreation.value = UiState.Loading
        chatRepository.createOrGetChannel(queryId, queryTitle, queryOwnerIds, helperIds) { state ->
            _channelCreation.value = state
        }
    }

    fun disconnectUser() {
        chatRepository.disconnectUser()
    }
}