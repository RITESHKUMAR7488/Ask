package com.example.ask.chatModule.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ask.chatModule.managers.CometChatManager
import com.example.ask.utilities.PreferenceManager
import com.example.ask.utilities.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val cometChatManager: CometChatManager,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _loginState = MutableLiveData<UiState<String>>()
    val loginState: LiveData<UiState<String>> = _loginState

    /**
     * Ensure the current user is logged into CometChat
     */
    fun ensureCometChatLogin(callback: (Boolean) -> Unit) {
        val userModel = preferenceManager.userModel

        if (userModel == null) {
            Log.e(TAG, "No user model found in preferences")
            _loginState.value = UiState.Failure("User not logged in")
            callback(false)
            return
        }

        // Check if already logged in to CometChat
        if (cometChatManager.isUserLoggedIn()) {
            val currentUser = cometChatManager.getCurrentUser()
            Log.d(TAG, "User already logged into CometChat: ${currentUser?.name}")
            _loginState.value = UiState.Success("Already logged in")
            callback(true)
            return
        }

        // Login to CometChat
        _loginState.value = UiState.Loading

        cometChatManager.loginUser(userModel) { success, error ->
            if (success) {
                Log.d(TAG, "CometChat login successful for user: ${userModel.fullName}")
                _loginState.postValue(UiState.Success("Login successful"))
                callback(true)
            } else {
                Log.e(TAG, "CometChat login failed: $error")
                _loginState.postValue(UiState.Failure(error ?: "Unknown error"))
                callback(false)
            }
        }
    }

    /**
     * Logout user from CometChat
     */
    fun logoutFromCometChat(callback: (Boolean) -> Unit) {
        if (!cometChatManager.isUserLoggedIn()) {
            Log.d(TAG, "User not logged into CometChat")
            callback(true)
            return
        }

        cometChatManager.logoutUser { success, error ->
            if (success) {
                Log.d(TAG, "CometChat logout successful")
                callback(true)
            } else {
                Log.e(TAG, "CometChat logout failed: $error")
                callback(false)
            }
        }
    }

    /**
     * Update user profile in CometChat
     */
    fun updateUserProfile(callback: (Boolean) -> Unit) {
        val userModel = preferenceManager.userModel

        if (userModel == null) {
            Log.e(TAG, "No user model found for profile update")
            callback(false)
            return
        }

        cometChatManager.updateUserProfile(userModel) { success, error ->
            if (success) {
                Log.d(TAG, "CometChat profile updated successfully")
                callback(true)
            } else {
                Log.e(TAG, "CometChat profile update failed: $error")
                callback(false)
            }
        }
    }

    /**
     * Check if user is logged into CometChat
     */
    fun isLoggedInToCometChat(): Boolean {
        return cometChatManager.isUserLoggedIn()
    }
}