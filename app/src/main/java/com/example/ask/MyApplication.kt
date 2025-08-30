package com.example.ask

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.cometchat.chat.core.AppSettings
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings
import com.example.ask.chatModule.config.CometChatConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {

    companion object {
        private const val TAG = "MyApplication"
        var isCometChatInitialized = false
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize CometChat directly in Application onCreate
        initializeCometChatDirectly()
    }

    private fun initializeCometChatDirectly() {
        if (isCometChatInitialized) {
            Log.d(TAG, "CometChat already initialized")
            return
        }

        // Verify configuration first
        if (!CometChatConfig.verifyConfig()) {
            Log.e(TAG, "❌ CometChat configuration is invalid!")
            return
        }

        Log.d(TAG, "🚀 Starting CometChat initialization...")
        Log.d(TAG, "App ID: ${CometChatConfig.APP_ID}")
        Log.d(TAG, "Region: ${CometChatConfig.REGION}")

        try {
            // 1. Initialize Core SDK first
            val appSettings = AppSettings.AppSettingsBuilder()
                .subscribePresenceForAllUsers()
                .setRegion(CometChatConfig.REGION)
                .autoEstablishSocketConnection(true)
                .build()

            CometChat.init(this, CometChatConfig.APP_ID, appSettings, object : CometChat.CallbackListener<String>() {
                override fun onSuccess(successMessage: String) {
                    Log.d(TAG, "✅ CometChat Core SDK initialized successfully: $successMessage")

                    // 2. Initialize UI Kit after Core with small delay to ensure core is ready
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            val uiKitSettings = UIKitSettings.UIKitSettingsBuilder()
                                .setRegion(CometChatConfig.REGION)
                                .setAppId(CometChatConfig.APP_ID)
                                .setAuthKey(CometChatConfig.AUTH_KEY)
                                .subscribePresenceForAllUsers()
                                .build()

                            CometChatUIKit.init(this@MyApplication, uiKitSettings, object : CometChat.CallbackListener<String?>() {
                                override fun onSuccess(successMessage: String?) {
                                    Log.d(TAG, "✅ CometChat UIKit initialized successfully: $successMessage")
                                    isCometChatInitialized = true
                                    Log.d(TAG, "✅ CometChat fully ready for UI components")
                                }

                                override fun onError(exception: CometChatException?) {
                                    Log.e(TAG, "❌ CometChat UIKit initialization failed")
                                    Log.e(TAG, "UIKit Error Code: ${exception?.code}")
                                    Log.e(TAG, "UIKit Error Message: ${exception?.message}")
                                    Log.e(TAG, "UIKit Error Details: ${exception?.details}")
                                    // Keep isCometChatInitialized = false on error
                                }
                            })
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Exception during UIKit initialization", e)
                        }
                    }, 200) // 200ms delay to ensure core is fully ready
                }

                override fun onError(e: CometChatException) {
                    Log.e(TAG, "❌ CometChat Core SDK initialization failed")
                    Log.e(TAG, "Core Error Code: ${e.code}")
                    Log.e(TAG, "Core Error Message: ${e.message}")
                    Log.e(TAG, "Core Error Details: ${e.details}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception during CometChat initialization", e)
        }
    }
}