// File: app/src/main/java/com/example/ask/chatModule/di/ChatModule.kt
package com.example.ask.chatModule.di

import com.example.ask.chatModule.repositories.ChatRepository
import com.example.ask.chatModule.repositories.ChatRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ChatModule {

    @Provides
    @Singleton
    fun provideChatRepository(
        firestore: FirebaseFirestore
    ): ChatRepository {
        return ChatRepositoryImpl(firestore)
    }
}