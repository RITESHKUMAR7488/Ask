package com.example.ask

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.example.ask.chatModule.managers.CometChatManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application() {

    @Inject
    lateinit var cometChatManager: CometChatManager

    companion object {
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()

        cometChatManager.initializeCometChat(this) { success, message ->
            if (success) {
                Log.d("MyApplication", "CometChat initialized successfully")
            } else {
                Log.e("MyApplication", "CometChat initialization failed: $message")
            }
        }
    }


    private fun initializeCometChat() {
        cometChatManager.initializeCometChat(this) { success, message ->
            if (success) {
                Log.d(TAG, "CometChat initialized successfully in Application: $message")
            } else {
                Log.e(TAG, "CometChat initialization failed in Application: $message")
            }
        }
    }
}