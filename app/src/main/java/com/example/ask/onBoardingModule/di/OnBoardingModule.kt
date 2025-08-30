package com.example.ask.onBoardingModule.di

import com.example.ask.chatModule.managers.CometChatManager
import com.example.ask.onBoardingModule.repositories.OnBoardingRepository
import com.example.ask.onBoardingModule.repositories.OnBoardingRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class OnBoardingModule {
    @Provides
    @Singleton
    fun provideOnBoardingRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        cometChatManager: CometChatManager
    ): OnBoardingRepository {
        return OnBoardingRepositoryImpl(firestore, auth, cometChatManager)
    }
}