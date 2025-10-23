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

    // --- FIX: Add LiveData for sending notifications ---
    private val _addNotificationState = MutableLiveData<UiState<String>>()
    val addNotificationState: LiveData<UiState<String>> = _addNotificationState
    // --- End of FIX ---


    fun getUserNotifications() {
        val userId = preferenceManager.userId
        if (userId != null) {
            repository.getUserNotifications(userId) {
                _userNotifications.value = it
            }
        } else {
            _userNotifications.value = UiState.Failure("User not logged in")
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            val userId = preferenceManager.userId
            if (userId != null) {
                val result = repository.markNotificationAsRead(userId, notificationId)
                if (result is UiState.Failure) {
                    _markAsReadState.value = result
                }
            } else {
                _markAsReadState.value = UiState.Failure("User not logged in")
            }
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            _markAllAsReadState.value = UiState.Loading
            val userId = preferenceManager.userId
            if (userId != null) {
                val result = repository.markAllNotificationsAsRead(userId)
                _markAllAsReadState.value = result
            } else {
                _markAllAsReadState.value = UiState.Failure("User not logged in")
            }
        }
    }

    // --- FIX: Add function to send a help notification ---
    /**
     * Creates and sends a "Help Request" notification to a target user.
     * This uses coroutines as per your preference by wrapping the callback.
     */
    // ... (inside NotificationViewModel class)

    // --- FIX: Add function to send a help notification ---
    fun sendHelpNotification(
        targetUserId: String,
        queryTitle: String,
        queryId: String,
        senderName: String,
        senderUserId: String,
        senderPhoneNumber: String?,
        senderEmail: String?,
        senderProfileImage: String? // <-- Add this parameter
    ) {
        _addNotificationState.value = UiState.Loading

        // --- FIX: Use the correct function name ---
        val notification = NotificationUtils.createHelpRequestNotification(
            targetUserId = targetUserId, // Pass targetUserId to the correct parameter
            queryTitle = queryTitle,
            queryId = queryId,
            senderUserName = senderName, // Pass senderName to senderUserName
            senderUserId = senderUserId,
            senderPhoneNumber = senderPhoneNumber,
            senderEmail = senderEmail,
            senderProfileImage = senderProfileImage // <-- Pass it here
        )

        repository.addNotification(targetUserId, notification) { state ->
            _addNotificationState.postValue(state)
        }
    }
// ... (rest of the class)
    // --- End of FIX ---
}