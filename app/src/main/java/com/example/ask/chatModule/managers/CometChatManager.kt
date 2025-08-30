package com.example.ask.chatModule.managers

import android.content.Context
import android.util.Log
import com.cometchat.chat.core.AppSettings
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings
import com.example.ask.MyApplication
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
        // Check if already initialized in MyApplication
        if (MyApplication.isCometChatInitialized) {
            Log.d(TAG, "CometChat already initialized in MyApplication")
            callback(true, "Already initialized")
            return
        }

        // If not initialized in Application (fallback), initialize here
        if (preferenceManager.mPreferences.getBoolean(Constant.COMETCHAT_INITIALIZED, false)) {
            Log.d(TAG, "CometChat already initialized (from preferences)")
            callback(true, null)
            return
        }

        Log.d(TAG, "Starting CometChat initialization...")
        Log.d(TAG, "App ID: ${CometChatConfig.APP_ID}")
        Log.d(TAG, "Region: ${CometChatConfig.REGION}")

        // Fallback initialization
        val appSettings = AppSettings.AppSettingsBuilder()
            .subscribePresenceForAllUsers()
            .setRegion(CometChatConfig.REGION)
            .build()

        CometChat.init(context, CometChatConfig.APP_ID, appSettings, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(successMessage: String) {
                Log.d(TAG, "CometChat Core SDK initialized (fallback): $successMessage")

                val uiKitSettings = UIKitSettings.UIKitSettingsBuilder()
                    .setRegion(CometChatConfig.REGION)
                    .setAppId(CometChatConfig.APP_ID)
                    .setAuthKey(CometChatConfig.AUTH_KEY)
                    .subscribePresenceForAllUsers()
                    .build()

                CometChatUIKit.init(context, uiKitSettings, object : CometChat.CallbackListener<String?>() {
                    override fun onSuccess(successMessage: String?) {
                        Log.d(TAG, "CometChat UIKit initialized (fallback): $successMessage")

                        preferenceManager.mPreferences.edit()
                            .putBoolean(Constant.COMETCHAT_INITIALIZED, true)
                            .apply()

                        callback(true, "CometChat initialized (Core + UI Kit)")
                    }

                    override fun onError(exception: CometChatException?) {
                        Log.e(TAG, "CometChat UIKit initialization failed (fallback)", exception)
                        Log.e(TAG, "UIKit Error Code: ${exception?.code}")
                        Log.e(TAG, "UIKit Error Message: ${exception?.message}")
                        callback(false, exception?.message ?: "UIKit init failed")
                    }
                })
            }

            override fun onError(e: CometChatException) {
                Log.e(TAG, "CometChat Core SDK initialization failed (fallback)", e)
                Log.e(TAG, "Core Error Code: ${e.code}")
                Log.e(TAG, "Core Error Message: ${e.message}")
                callback(false, e.message ?: "Core init failed")
            }
        })
    }

    fun waitForInitialization(callback: (Boolean) -> Unit) {
        // Check if initialized
        if (MyApplication.isCometChatInitialized) {
            Log.d(TAG, "CometChat initialization check: Already initialized")
            callback(true)
            return
        }

        Log.d(TAG, "Waiting for CometChat initialization...")

        // Wait for up to 10 seconds for initialization (increased from 5)
        val maxWaitTime = 10000 // 10 seconds
        val checkInterval = 500 // 500ms
        var waitedTime = 0

        val checkRunnable = object : Runnable {
            override fun run() {
                if (MyApplication.isCometChatInitialized) {
                    Log.d(TAG, "CometChat initialization completed after ${waitedTime}ms")
                    callback(true)
                } else if (waitedTime < maxWaitTime) {
                    waitedTime += checkInterval
                    Log.d(TAG, "Still waiting for initialization... (${waitedTime}ms)")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, checkInterval.toLong())
                } else {
                    Log.e(TAG, "CometChat initialization timeout after ${waitedTime}ms")
                    callback(false) // Timeout
                }
            }
        }

        android.os.Handler(android.os.Looper.getMainLooper()).post(checkRunnable)
    }

    fun createCometChatUser(userModel: UserModel, callback: (Boolean, String?) -> Unit) {
        val cometChatUserId = CometChatConfig.getCometChatUserId(userModel.uid ?: "")

        if (userModel.uid.isNullOrEmpty()) {
            Log.e(TAG, "Cannot create CometChat user: Firebase UID is null or empty")
            callback(false, "User ID is required")
            return
        }

        Log.d(TAG, "Creating CometChat user...")
        Log.d(TAG, "Firebase UID: ${userModel.uid}")
        Log.d(TAG, "CometChat User ID: $cometChatUserId")
        Log.d(TAG, "User Name: ${userModel.fullName}")
        Log.d(TAG, "User Email: ${userModel.email}")
        Log.d(TAG, "Original Avatar: ${userModel.imageUrl}")

        // ✅ Provide default avatar if user doesn't have one
        val avatarUrl = if (userModel.imageUrl.isNullOrEmpty()) {
            "https://via.placeholder.com/150/4CAF50/FFFFFF?text=${userModel.fullName?.firstOrNull()?.uppercase() ?: "U"}"
        } else {
            userModel.imageUrl!!
        }

        Log.d(TAG, "Using Avatar: $avatarUrl")

        val user = User().apply {
            uid = cometChatUserId
            name = userModel.fullName ?: "Unknown User"
            avatar = avatarUrl  // ✅ Always provide a valid avatar URL

            // Add metadata
            val metadataJson = org.json.JSONObject()
            try {
                metadataJson.put("email", userModel.email ?: "")
                metadataJson.put("phone", userModel.mobileNumber ?: "")
                metadataJson.put("firebase_uid", userModel.uid ?: "")
                metadata = metadataJson
                Log.d(TAG, "User metadata: ${metadataJson.toString()}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set user metadata", e)
            }
        }

        CometChat.createUser(user, CometChatConfig.AUTH_KEY, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(createdUser: User) {
                Log.d(TAG, "✅ CometChat user created successfully!")
                Log.d(TAG, "Created User ID: ${createdUser.uid}")
                Log.d(TAG, "Created User Name: ${createdUser.name}")
                Log.d(TAG, "Created User Avatar: ${createdUser.avatar}")

                preferenceManager.mPreferences.edit()
                    .putBoolean(Constant.COMETCHAT_USER_CREATED, true)
                    .apply()

                callback(true, "User created successfully")
            }

            override fun onError(exception: CometChatException) {
                Log.e(TAG, "❌ CometChat user creation failed")
                Log.e(TAG, "Error Code: ${exception.code}")
                Log.e(TAG, "Error Message: ${exception.message}")
                Log.e(TAG, "Error Details: ${exception.details}")

                // Check if user already exists
                if (exception.code == "ERR_UID_ALREADY_EXISTS") {
                    Log.d(TAG, "✅ CometChat user already exists: $cometChatUserId")

                    preferenceManager.mPreferences.edit()
                        .putBoolean(Constant.COMETCHAT_USER_CREATED, true)
                        .apply()

                    callback(true, "User already exists")
                } else {
                    callback(false, "Failed to create user: ${exception.message}")
                }
            }
        })
    }

    fun loginToCometChat(userId: String, callback: (Boolean, User?, String?) -> Unit) {
        val cometChatUserId = CometChatConfig.getCometChatUserId(userId)

        Log.d(TAG, "Attempting CometChat login...")
        Log.d(TAG, "Firebase UID: $userId")
        Log.d(TAG, "CometChat User ID: $cometChatUserId")

        CometChatUIKit.login(cometChatUserId, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User) {
                Log.d(TAG, "✅ CometChat login successful!")
                Log.d(TAG, "Logged in User ID: ${user.uid}")
                Log.d(TAG, "Logged in User Name: ${user.name}")
                callback(true, user, null)
            }

            override fun onError(exception: CometChatException) {
                Log.e(TAG, "❌ CometChat login failed")
                Log.e(TAG, "Login Error Code: ${exception.code}")
                Log.e(TAG, "Login Error Message: ${exception.message}")
                Log.e(TAG, "Login Error Details: ${exception.details}")
                callback(false, null, "Login failed: ${exception.message}")
            }
        })
    }

    fun logoutFromCometChat(callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "Attempting CometChat logout...")

        CometChat.logout(object : CometChat.CallbackListener<String>() {
            override fun onSuccess(message: String) {
                Log.d(TAG, "✅ CometChat logout successful: $message")
                callback(true, message)
            }

            override fun onError(exception: CometChatException) {
                Log.e(TAG, "❌ CometChat logout failed", exception)
                callback(false, exception.message ?: "Logout failed")
            }
        })
    }

    fun getCurrentCometChatUser(): User? {
        val user = CometChat.getLoggedInUser()
        if (user != null) {
            Log.d(TAG, "Current CometChat user: ${user.uid} - ${user.name}")
        } else {
            Log.d(TAG, "No current CometChat user")
        }
        return user
    }

    fun isCometChatLoggedIn(): Boolean {
        val isLoggedIn = CometChat.getLoggedInUser() != null
        Log.d(TAG, "CometChat login status: $isLoggedIn")
        return isLoggedIn
    }
}