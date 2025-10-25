package com.example.ask.notificationModule.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ask.notificationModule.models.NotificationModel
import com.example.ask.notificationModule.repositories.NotificationRepository
// Import the NotificationUtils
import com.example.ask.notificationModule.utils.NotificationUtils
import com.example.ask.utilities.PreferenceManager
import com.example.ask.utilities.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _userNotifications = MutableLiveData<UiState<List<NotificationModel>>>()
    val userNotifications: LiveData<UiState<List<NotificationModel>>> = _userNotifications

    private val _markAsReadState = MutableLiveData<UiState<Unit>>()
    val markAsReadState: LiveData<UiState<Unit>> = _markAsReadState

    private val _markAllAsReadState = MutableLiveData<UiState<Unit>>()
    val markAllAsReadState: LiveData<UiState<Unit>> = _markAllAsReadState

    private val _addNotificationState = MutableLiveData<UiState<String>>()
    val addNotificationState: LiveData<UiState<String>> = _addNotificationState


    fun getUserNotifications() {
        val userId = preferenceManager.userId
        if (userId != null && userId.isNotEmpty()) { // Check for not empty
            repository.getUserNotifications(userId) {
                _userNotifications.value = it
            }
        } else {
            _userNotifications.value = UiState.Failure("User not logged in")
        }
    }

    /**
     * --- COROUTINE USAGE ---
     * This function uses 'viewModelScope.launch' to start a new coroutine
     * on the main thread. Inside the coroutine, it calls the 'suspend' function
     * 'repository.markNotificationAsRead'. This allows the UI to remain responsive
     * while the background database operation completes.
     */
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            val userId = preferenceManager.userId
            if (userId != null && userId.isNotEmpty()) {
                // The repository function is a suspend function and is safely
                // called within the coroutine.
                val result = repository.markNotificationAsRead(userId, notificationId)
                if (result is UiState.Failure) {
                    _markAsReadState.value = result
                }
                // Success is handled by the snapshot listener in getUserNotifications
            } else {
                _markAsReadState.value = UiState.Failure("User not logged in")
            }
        }
    }

    /**
     * --- COROUTINE USAGE ---
     * This function also uses 'viewModelScope.launch' to run a background task.
     * It calls the 'suspend' function 'repository.markAllNotificationsAsRead'.
     * This is crucial because marking all notifications could be a heavy
     * operation, and doing it on the main thread would freeze the app.
     */
    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            _markAllAsReadState.value = UiState.Loading
            val userId = preferenceManager.userId
            if (userId != null && userId.isNotEmpty()) {
                // Safely calling the suspend function
                val result = repository.markAllNotificationsAsRead(userId)
                _markAllAsReadState.value = result
            } else {
                _markAllAsReadState.value = UiState.Failure("User not logged in")
            }
        }
    }

    /**
     * Creates and sends a "Help Request" notification to a target user.
     * This function itself doesn't need to be a suspend function because
     * the repository's 'addNotification' uses a callback, which is already
     * asynchronous.
     */
    fun sendHelpNotification(
        targetUserId: String,
        queryTitle: String,
        queryId: String,
        communityId: String, // <-- FIX: Added this parameter
        senderName: String,
        senderUserId: String,
        senderPhoneNumber: String?,
        senderEmail: String?,
        senderProfileImage: String?
    ) {
        _addNotificationState.value = UiState.Loading

        val notification = NotificationUtils.createHelpRequestNotification(
            targetUserId = targetUserId,
            queryTitle = queryTitle,
            queryId = queryId,
            communityId = communityId, // <-- FIX: Pass it here
            senderUserName = senderName,
            senderUserId = senderUserId,
            senderPhoneNumber = senderPhoneNumber,
            senderEmail = senderEmail,
            senderProfileImage = senderProfileImage
        )

        // The repository handles this asynchronously with a callback
        repository.addNotification(targetUserId, notification) { state ->
            _addNotificationState.postValue(state) // Use postValue if called from bg thread
        }
    }
}