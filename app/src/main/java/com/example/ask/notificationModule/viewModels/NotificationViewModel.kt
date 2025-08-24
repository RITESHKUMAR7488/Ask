package com.example.ask.notificationModule.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ask.notificationModule.models.NotificationModel
import com.example.ask.notificationModule.repositories.NotificationRepository
import com.example.ask.utilities.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _addNotification = MutableLiveData<UiState<String>>()
    val addNotification: LiveData<UiState<String>> = _addNotification

    private val _userNotifications = MutableLiveData<UiState<List<NotificationModel>>>()
    val userNotifications: LiveData<UiState<List<NotificationModel>>> = _userNotifications

    private val _unreadCount = MutableLiveData<UiState<Int>>()
    val unreadCount: LiveData<UiState<Int>> = _unreadCount

    fun sendHelpNotification(
        targetUserId: String,
        queryTitle: String,
        senderName: String,
        senderUserId: String,
        queryId: String
    ) {
        val notification = NotificationModel(
            userId = targetUserId,
            title = "Help Request",
            message = "$senderName is requesting help with: $queryTitle",
            type = "HELP_REQUEST",
            queryId = queryId,
            senderUserId = senderUserId,
            senderUserName = senderName
        )

        _addNotification.value = UiState.Loading
        repository.addNotification(notification) {
            _addNotification.value = it
        }
    }

    fun getUserNotifications(userId: String) {
        _userNotifications.value = UiState.Loading
        repository.getUserNotifications(userId) {
            _userNotifications.value = it
        }
    }

    fun getUnreadNotificationCount(userId: String) {
        repository.getUnreadNotificationCount(userId) {
            _unreadCount.value = it
        }
    }

    fun removeNotificationListener() {
        repository.removeNotificationListener()
    }

    override fun onCleared() {
        super.onCleared()
        repository.removeNotificationListener()
    }
}
