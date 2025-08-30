package com.example.ask.chatModule.managers

import android.content.Context
import android.util.Log
import com.cometchat.chat.core.AppSettings
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings
import com.example.ask.chatModule.config.CometChatConfig
import com.example.ask.onBoardingModule.models.UserModel
import com.example.ask.utilities.Constant
import com.example.ask.utilities.PreferenceManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CometChatManager @Inject constructor(
    private val preferenceManager: PreferenceManager
) {
    companion object {
        private const val TAG = "CometChatManager"
    }

    fun initializeCometChat(context: Context, callback: (Boolean, String?) -> Unit) {
        if (preferenceManager.mPreferences.getBoolean(Constant.COMETCHAT_INITIALIZED, false)) {
            Log.d(TAG, "CometChat already initialized")
            callback(true, null)
            return
        }

        // 1. Initialize Core SDK
        val appSettings = AppSettings.AppSettingsBuilder()
            .subscribePresenceForAllUsers()
            .setRegion(CometChatConfig.REGION)
            .build()

        CometChat.init(context, CometChatConfig.APP_ID, appSettings, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(successMessage: String) {
                Log.d(TAG, "CometChat Core SDK initialized: $successMessage")

                // 2. Initialize UI Kit after Core
                val uiKitSettings = UIKitSettings.UIKitSettingsBuilder()
                    .setRegion(CometChatConfig.REGION)
                    .setAppId(CometChatConfig.APP_ID)
                    .setAuthKey(CometChatConfig.AUTH_KEY)
                    .subscribePresenceForAllUsers()
                    .build()

                CometChatUIKit.init(context, uiKitSettings, object : CometChat.CallbackListener<String?>() {
                    override fun onSuccess(successMessage: String?) {
                        Log.d(TAG, "CometChat UIKit initialized: $successMessage")

                        // Mark as initialized
                        preferenceManager.mPreferences.edit()
                            .putBoolean(Constant.COMETCHAT_INITIALIZED, true)
                            .apply()

                        callback(true, "CometChat initialized (Core + UI Kit)")
                    }

                    override fun onError(exception: CometChatException?) {
                        Log.e(TAG, "CometChat UIKit initialization failed", exception)
                        callback(false, exception?.message ?: "UIKit init failed")
                    }
                })
            }

            override fun onError(e: CometChatException) {
                Log.e(TAG, "CometChat Core SDK initialization failed", e)
                callback(false, e.message ?: "Core init failed")
            }
        })
    }


    fun createCometChatUser(userModel: UserModel, callback: (Boolean, String?) -> Unit) {
        val cometChatUserId = CometChatConfig.getCometChatUserId(userModel.uid ?: return)

        Log.d(TAG, "Creating CometChat user with ID: $cometChatUserId")

        val user = User().apply {
            uid = cometChatUserId
            name = userModel.fullName ?: "Unknown User"
            avatar = userModel.imageUrl ?: ""
            // Optional: Add more metadata
            val metadataJson = org.json.JSONObject()
            metadataJson.put("email", userModel.email ?: "")
            metadataJson.put("phone", userModel.mobileNumber ?: "")
            metadataJson.put("firebase_uid", userModel.uid ?: "")
            metadata = metadataJson
        }

        CometChat.createUser(user, CometChatConfig.AUTH_KEY, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(createdUser: User) {
                Log.d(TAG, "CometChat user created successfully: ${createdUser.uid}")

                // Mark user as created
                preferenceManager.mPreferences.edit()
                    .putBoolean(Constant.COMETCHAT_USER_CREATED, true)
                    .apply()

                callback(true, "User created successfully")
            }

            override fun onError(exception: CometChatException) {
                // Check if user already exists
                if (exception.code == "ERR_UID_ALREADY_EXISTS") {
                    Log.d(TAG, "CometChat user already exists: $cometChatUserId")

                    // Mark as created even if already exists
                    preferenceManager.mPreferences.edit()
                        .putBoolean(Constant.COMETCHAT_USER_CREATED, true)
                        .apply()

                    callback(true, "User already exists")
                } else {
                    Log.e(TAG, "Failed to create CometChat user", exception)
                    callback(false, exception.message ?: "Failed to create user")
                }
            }
        })
    }

    fun loginToCometChat(userId: String, callback: (Boolean, User?, String?) -> Unit) {
        val cometChatUserId = CometChatConfig.getCometChatUserId(userId)

        Log.d(TAG, "Logging in to CometChat with user ID: $cometChatUserId")

        CometChatUIKit.login(cometChatUserId, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User) {
                Log.d(TAG, "CometChat login successful for user: ${user.uid}")
                callback(true, user, null)
            }

            override fun onError(exception: CometChatException) {
                Log.e(TAG, "CometChat login failed", exception)
                callback(false, null, exception.message ?: "Login failed")
            }
        })
    }

    fun logoutFromCometChat(callback: (Boolean, String?) -> Unit) {
        CometChat.logout(object : CometChat.CallbackListener<String>() {
            override fun onSuccess(message: String) {
                Log.d(TAG, "CometChat logout successful: $message")
                callback(true, message)
            }

            override fun onError(exception: CometChatException) {
                Log.e(TAG, "CometChat logout failed", exception)
                callback(false, exception.message ?: "Logout failed")
            }
        })
    }

    fun getCurrentCometChatUser(): User? {
        return CometChat.getLoggedInUser()
    }

    fun isCometChatLoggedIn(): Boolean {
        return CometChat.getLoggedInUser() != null
    }
}