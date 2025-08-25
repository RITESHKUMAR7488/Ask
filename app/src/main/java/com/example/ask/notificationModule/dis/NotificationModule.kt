package com.example.ask.notificationModule.dis

import com.example.ask.notificationModule.repositories.NotificationRepository
import com.example.ask.notificationModule.repositories.NotificationRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NotificationModule {

    @Provides
    @Singleton
    fun provideNotificationRepository(
        firestore: FirebaseFirestore
    ): NotificationRepository {
        return NotificationRepositoryImpl(firestore)
    }
}