package com.example.ask.chatModule.di

import com.example.ask.chatModule.managers.CometChatManager
import com.example.ask.utilities.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class ChatModule {

    @Provides
    @Singleton
    fun provideCometChatManager(preferenceManager: PreferenceManager): CometChatManager {
        return CometChatManager(preferenceManager)
    }
}