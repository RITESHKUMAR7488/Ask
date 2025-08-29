package com.example.ask.chatModule.managers

import android.util.Log
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.example.ask.onBoardingModule.models.UserModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CometChatManager @Inject constructor() {

    companion object {
        private const val TAG = "CometChatManager"
        // Replace with your actual CometChat Auth Key
        private const val AUTH_KEY = "YOUR_AUTH_KEY_HERE"
    }

    /**
     * Create and login user to CometChat
     */
    fun loginUser(userModel: UserModel, callback: (Boolean, String?) -> Unit) {
        val uid = userModel.uid ?: return callback(false, "User ID is null")

        // First, try to login the user
        CometChat.login(uid, AUTH_KEY, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User) {
                Log.d(TAG, "Login Successful: ${user.name}")
                callback(true, null)
            }

            override fun onError(exception: CometChatException) {
                Log.e(TAG, "Login failed: ${exception.message}")

                // If login fails, try to create user first
                if (exception.code == "ERR_UID_NOT_FOUND") {
                    createUser(userModel) { success, error ->
                        if (success) {
                            // After creating user, login again
                            loginUser(userModel, callback)
                        } else {
                            callback(false, error)
                        }
                    }
                } else {
                    callback(false, exception.message)
                }
            }
        })
    }

    /**
     * Create user in CometChat
     */
    private fun createUser(userModel: UserModel, callback: (Boolean, String?) -> Unit) {
        val user = User().apply {
            uid = userModel.uid
            name = userModel.fullName ?: "Unknown User"
            avatar = userModel.imageUrl
        }

        CometChat.createUser(user, AUTH_KEY, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User) {
                Log.d(TAG, "User created successfully: ${user.name}")
                callback(true, null)
            }

            override fun onError(exception: CometChatException) {
                Log.e(TAG, "User creation failed: ${exception.message}")
                callback(false, exception.message)
            }
        })
    }

    /**
     * Logout user from CometChat
     */
    fun logoutUser(callback: (Boolean, String?) -> Unit) {
        CometChat.logout(object : CometChat.CallbackListener<String>() {
            override fun onSuccess(message: String) {
                Log.d(TAG, "Logout completed successfully: $message")
                callback(true, null)
            }

            override fun onError(exception: CometChatException) {
                Log.e(TAG, "Logout failed: ${exception.message}")
                callback(false, exception.message)
            }
        })
    }

    /**
     * Get current logged in user
     */
    fun getCurrentUser(): User? {
        return CometChat.getLoggedInUser()
    }

    /**
     * Check if user is logged in to CometChat
     */
    fun isUserLoggedIn(): Boolean {
        return CometChat.getLoggedInUser() != null
    }

    /**
     * Update user profile in CometChat
     */
    fun updateUserProfile(userModel: UserModel, callback: (Boolean, String?) -> Unit) {
        val user = User().apply {
            uid = userModel.uid
            name = userModel.fullName
            avatar = userModel.imageUrl
        }

        CometChat.updateUser(user, AUTH_KEY, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User) {
                Log.d(TAG, "User updated successfully: ${user.name}")
                callback(true, null)
            }

            override fun onError(exception: CometChatException) {
                Log.e(TAG, "User update failed: ${exception.message}")
                callback(false, exception.message)
            }
        })
    }
}