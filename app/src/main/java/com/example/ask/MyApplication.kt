package com.example.ask

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.cometchat.chat.core.AppSettings
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : MultiDexApplication() {

    companion object {
        // Replace with your actual CometChat App ID, Auth Key, and Region
        private const val COMETCHAT_APP_ID = "YOUR_APP_ID_HERE"
        private const val COMETCHAT_AUTH_KEY = "YOUR_AUTH_KEY_HERE"
        private const val COMETCHAT_REGION = "YOUR_REGION_HERE" // e.g., "us" or "eu"
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize MultiDex
        MultiDex.install(this)

        // Set default night mode to disabled
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Initialize CometChat
        initializeCometChat()
    }

    private fun initializeCometChat() {
        val appSettings = AppSettings.AppSettingsBuilder()
            .subscribePresenceForAllUsers()
            .setRegion(COMETCHAT_REGION)
            .autoEstablishSocketConnection(true)
            .build()

        CometChat.init(this, COMETCHAT_APP_ID, appSettings, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(successMessage: String) {
                android.util.Log.d(TAG, "CometChat initialization completed successfully: $successMessage")
            }

            override fun onError(e: CometChatException) {
                android.util.Log.e(TAG, "CometChat initialization failed with exception: ${e.message}", e)
            }
        })
    }
}