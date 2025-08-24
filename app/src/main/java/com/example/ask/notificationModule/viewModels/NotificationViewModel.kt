package com.example.ask.notificationModule.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ask.notificationModule.models.NotificationModel
import com.example.ask.notificationModule.repositories.NotificationRepository
import com.example.ask.notificationModule.utils.NotificationUtils
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

    private val _markAsRead = MutableLiveData<UiState<String>>()
    val markAsRead: LiveData<UiState<String>> = _markAsRead

    /**
     * ✅ NEW: Send help notification with contact details
     */
    fun sendHelpNotification(
        targetUserId: String,
        queryTitle: String,
        queryId: String,
        senderName: String,
        senderUserId: String,
        senderPhoneNumber: String? = null,
        senderEmail: String? = null,
        senderProfileImage: String? = null
    ) {
        val notification = NotificationUtils.createHelpRequestNotification(
            targetUserId = targetUserId,
            queryTitle = queryTitle,
            queryId = queryId,
            senderUserId = senderUserId,
            senderUserName = senderName,
            senderPhoneNumber = senderPhoneNumber,
            senderEmail = senderEmail,
            senderProfileImage = senderProfileImage
        )

        _addNotification.value = UiState.Loading
        repository.addNotification(notification) {
            _addNotification.value = it
        }
    }

    fun sendQueryUpdateNotification(
        targetUserId: String,
        queryTitle: String,
        queryId: String,
        updateMessage: String,
        senderUserId: String? = null,
        senderUserName: String? = null
    ) {
        val notification = NotificationUtils.createQueryUpdateNotification(
            targetUserId = targetUserId,
            queryTitle = queryTitle,
            queryId = queryId,
            updateMessage = updateMessage,
            senderUserId = senderUserId,
            senderUserName = senderUserName
        )

        _addNotification.value = UiState.Loading
        repository.addNotification(notification) {
            _addNotification.value = it
        }
    }

    fun sendResponseNotification(
        targetUserId: String,
        queryTitle: String,
        queryId: String,
        senderUserId: String,
        senderUserName: String,
        senderProfileImage: String? = null
    ) {
        val notification = NotificationUtils.createResponseNotification(
            targetUserId = targetUserId,
            queryTitle = queryTitle,
            queryId = queryId,
            senderUserId = senderUserId,
            senderUserName = senderUserName,
            senderProfileImage = senderProfileImage
        )

        _addNotification.value = UiState.Loading
        repository.addNotification(notification) {
            _addNotification.value = it
        }
    }

    fun sendCommunityInviteNotification(
        targetUserId: String,
        communityName: String,
        communityId: String,
        senderUserId: String,
        senderUserName: String,
        senderProfileImage: String? = null
    ) {
        val notification = NotificationUtils.createCommunityInviteNotification(
            targetUserId = targetUserId,
            communityName = communityName,
            communityId = communityId,
            senderUserId = senderUserId,
            senderUserName = senderUserName,
            senderProfileImage = senderProfileImage
        )

        _addNotification.value = UiState.Loading
        repository.addNotification(notification) {
            _addNotification.value = it
        }
    }

    fun sendCustomNotification(
        targetUserId: String,
        title: String,
        message: String,
        type: String = "GENERAL",
        queryId: String? = null,
        communityId: String? = null,
        senderUserId: String? = null,
        senderUserName: String? = null,
        actionData: String? = null
    ) {
        val notification = NotificationModel(
            userId = targetUserId,
            title = title,
            message = message,
            type = type,
            queryId = queryId,
            communityId = communityId,
            senderUserId = senderUserId,
            senderUserName = senderUserName,
            actionData = actionData,
            timestamp = System.currentTimeMillis(),
            isRead = false
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

    fun markNotificationAsRead(notificationId: String) {
        _markAsRead.value = UiState.Loading
        repository.markNotificationAsRead(notificationId) {
            _markAsRead.value = it
        }
    }

    fun getUnreadNotificationCount(userId: String) {
        repository.getUnreadNotificationCount(userId) {
            _unreadCount.value = it
        }
    }

    fun refreshNotifications(userId: String) {
        // Refresh both notifications and unread count
        getUserNotifications(userId)
        getUnreadNotificationCount(userId)
    }

    fun removeNotificationListener() {
        repository.removeNotificationListener()
    }

    override fun onCleared() {
        super.onCleared()
        repository.removeNotificationListener()
    }
}