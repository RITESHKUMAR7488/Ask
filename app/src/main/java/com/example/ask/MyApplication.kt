package com.example.ask

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Initialize Stream Chat
        initializeStreamChat()
    }

    private fun initializeStreamChat() {
        // Replace with your Stream API key from dashboard
        val apiKey = "YOUR_STREAM_API_KEY" // Get this from Stream Dashboard

        // State plugin for handling state management
        val statePluginFactory = StreamStatePluginFactory(
            config = StatePluginConfig(
                backgroundSyncEnabled = true,
                userPresence = true
            ),
            appContext = this
        )

        // Offline plugin for offline support
        val offlinePluginFactory = StreamOfflinePluginFactory(
            appContext = this
        )

        ChatClient.Builder(apiKey, this)
            .withPlugins(offlinePluginFactory, statePluginFactory)
            .logLevel(ChatLogLevel.ALL)
            .build()
    }
}